appName=$1
extensions=$2
/opt/homebrew/bin/quarkus create app com.demo:$appName --extension $extensions --gradle-kotlin-dsl
