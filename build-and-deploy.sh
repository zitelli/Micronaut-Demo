cd ~/Documentos/Alura/micronaut-demo

# 1. Gere a tag
TAG=$(date +%Y%m%d-%H%M%S)

# 2. Reconstrua o JAR e a imagem
mvn clean package -DskipTests
docker build --no-cache -t micronaut-demo:$TAG .

# 3. Carregue no Minikube
minikube image load micronaut-demo:$TAG

# 4. Atualize a tag no deployment.yml
sed -i "s|image: micronaut-demo:.*|image: micronaut-demo:$TAG|" k8s/deployment.yml

# 5. Aplique
kubectl apply -f k8s/deployment.yml

# 6. Acompanhe
kubectl get pods -w
