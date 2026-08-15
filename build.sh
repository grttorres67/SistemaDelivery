#!/usr/bin/env bash
set -e

mkdir -p build
kotlinc src/common/DeliveryCore.kt src/restaurante/RestauranteApp.kt -include-runtime -d build/restaurante.jar
kotlinc src/common/DeliveryCore.kt src/cliente/ClienteApp.kt -include-runtime -d build/cliente.jar

echo "Compilação concluída."
echo "Executar restaurante: java -jar build/restaurante.jar"
echo "Executar cliente: java -jar build/cliente.jar"
