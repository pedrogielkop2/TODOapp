package com.example.todoapp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoApp()
        }
    }
}

@Composable
fun TodoApp() {
    var tarefa by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf(listOf<Tarefa>()) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedTextField(
                    value = tarefa,
                    onValueChange = { tarefa = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    placeholder = { Text("Nova tarefa") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    if (tarefa.isNotBlank()) {
                        lista = lista + Tarefa(tarefa, false)
                        tarefa = ""
                        focusRequester.requestFocus() // volta o foco para digitar outra
                    }
                }) {
                    Text("Add")
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(lista) { item ->
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
                                onCheckedChange = { marcado ->
                                    lista = lista.map {
                                        if (it === item) it.copy(feito = marcado) else it
                                    }
                                }
                            )
                            Text(item.nome)
                        }

                        Button(onClick = { lista = lista - item }) {
                            Text("Remover")
                        }
                    }
                }
            }
        }
    }
}

data class Tarefa(
    val nome: String,
    val feito: Boolean
)

@Preview(showBackground = true)
@Composable
fun PreviewTodoApp() {
    MaterialTheme {
        TodoApp()
    }
}
