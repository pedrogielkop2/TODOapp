package com.example.todoapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Tarefa(
    val id: Long = System.currentTimeMillis(),
    val nome: String,
    val feito: Boolean
)

class TarefaRepository(private val context: Context) {

    private val fileName = "tarefas.json"

    fun salvar(lista: List<Tarefa>) {
        val jsonArray = JSONArray()
        lista.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("nome", it.nome)
            obj.put("feito", it.feito)
            jsonArray.put(obj)
        }
        File(context.filesDir, fileName).writeText(jsonArray.toString())
    }

    fun carregar(): List<Tarefa> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        val array = JSONArray(file.readText())
        val lista = mutableListOf<Tarefa>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                Tarefa(
                    id = obj.getLong("id"),
                    nome = obj.getString("nome"),
                    feito = obj.getBoolean("feito")
                )
            )
        }
        return lista
    }
}

class TarefaViewModel(private val repo: TarefaRepository) : ViewModel() {

    var tarefas by mutableStateOf(listOf<Tarefa>())
        private set

    var carregando by mutableStateOf(false)
        private set

    fun carregar() {
        viewModelScope.launch(Dispatchers.IO) {
            carregando = true
            tarefas = repo.carregar()
            carregando = false
        }
    }

    fun adicionar(nome: String) {
        val novaLista = tarefas + Tarefa(nome = nome, feito = false)
        salvar(novaLista)
    }

    fun atualizar(tarefa: Tarefa) {
        salvar(tarefas.map { if (it.id == tarefa.id) tarefa else it })
    }

    fun remover(tarefa: Tarefa) {
        salvar(tarefas - tarefa)
    }

    fun buscar(texto: String) {
        viewModelScope.launch(Dispatchers.Default) {
            carregando = true
            tarefas = repo.carregar().filter {
                it.nome.contains(texto, ignoreCase = true)
            }
            carregando = false
        }
    }

    private fun salvar(lista: List<Tarefa>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.salvar(lista)
            tarefas = lista
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TarefaViewModel(
                        TarefaRepository(this@MainActivity)
                    ) as T
                }
            }
        )[TarefaViewModel::class.java]

        viewModel.carregar()

        setContent {
            MaterialTheme {
                TodoApp(viewModel)
            }
        }
    }
}

@Composable
fun TodoApp(viewModel: TarefaViewModel) {

    var novaTarefa by remember { mutableStateOf("") }
    var busca by remember { mutableStateOf("") }
    var confirmarExclusao by remember { mutableStateOf<Tarefa?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        OutlinedTextField(
            value = busca,
            onValueChange = {
                busca = it
                viewModel.buscar(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar tarefa") }
        )

        if (viewModel.carregando) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = novaTarefa,
                onValueChange = { novaTarefa = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nova tarefa") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (novaTarefa.isNotBlank()) {
                    viewModel.adicionar(novaTarefa)
                    novaTarefa = ""
                }
            }) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(viewModel.tarefas) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.feito,
                            onCheckedChange = {
                                viewModel.atualizar(
                                    item.copy(feito = it)
                                )
                            }
                        )
                        Text(item.nome)
                    }

                    Button(onClick = { confirmarExclusao = item }) {
                        Text("Remover")
                    }
                }
            }
        }
    }

    confirmarExclusao?.let {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = null },
            title = { Text("Confirmação") },
            text = { Text("Deseja excluir esta tarefa?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.remover(it)
                    confirmarExclusao = null
                }) {
                    Text("Sim")
                }
            },
            dismissButton = {
                Button(onClick = { confirmarExclusao = null }) {
                    Text("Não")
                }
            }
        )
    }
}
