## Running Akka Cluster in Minikube

### Prerequisites

* [Maven](https://maven.apache.org/)
* [Docker](https://www.docker.com/)
* [VirtualBox](https://www.virtualbox.org)
* [Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/)

### Recommended Reading

* [Deploying clustered Akka applications on Kubernetes](https://developer.lightbend.com/guides/akka-cluster-kubernetes-k8s-deploy/)

## Install VirtualBox

https://www.virtualbox.org/wiki/Downloads

### Install kubectl

```brew install kubectl```

### Install Minikube

```
curl -Lo minikube https://storage.googleapis.com/minikube/releases/v0.22.3/minikube-darwin-amd64 && chmod +x minikube && sudo mv minikube /usr/local/bin/

minikube start --memory 5120 --cpus=4
```

### Deploy Akka Cluster to Minikube
```
deploy/kubernetes/scripts/install --minikube --new-minikube --all
```
#### Output:
```
****************************
***  Deploying customer   ***
****************************
service "customerservice-akka-remoting" created
service "customerservice" created
statefulset "customerservice" created
waiting......
****************************
***  Deploying nginx     ***
****************************
ingress "customer-ingress" created
deployment "nginx-default-backend" created
service "nginx-default-backend" created
deployment "nginx-ingress-controller" created
service "nginx-ingress" created
waiting.............................
NAME                                          READY     STATUS    RESTARTS   AGE
po/cassandra-0                                1/1       Running   0          3m
po/customerservice-0                          1/1       Running   0          34s
po/customerservice-1                          1/1       Running   0          33s
po/customerservice-2                          1/1       Running   0          32s
po/nginx-default-backend-1866436208-lcgxk     1/1       Running   0          30s
po/nginx-ingress-controller-667491271-t77sl   1/1       Running   0          30s

NAME                                CLUSTER-IP   EXTERNAL-IP   PORT(S)                      AGE
svc/cassandra                       10.0.0.213   <none>        9042/TCP                     3m
svc/customerservice                 None         <none>        9000/TCP                     34s
svc/customerservice-akka-remoting   10.0.0.209   <none>        2551/TCP                     34s
svc/kubernetes                      10.0.0.1     <none>        443/TCP                      3m
svc/nginx-default-backend           10.0.0.45    <none>        80/TCP                       30s
svc/nginx-ingress                   10.0.0.149   <pending>     80:30545/TCP,443:32276/TCP   30s

NAME                           DESIRED   CURRENT   AGE
statefulsets/cassandra         1         1         3m
statefulsets/customerservice   3         3         34s

NAME                              DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
deploy/nginx-default-backend      1         1         1            1           30s
deploy/nginx-ingress-controller   1         1         1            1           30s

NAME                                    DESIRED   CURRENT   READY     AGE
rs/nginx-default-backend-1866436208     1         1         1         30s
rs/nginx-ingress-controller-667491271   1         1         1         30s


Customer Service (HTTP): http://192.168.99.100:30545
Customer Service (HTTPS): https://192.168.99.100:32276
Kubernetes Dashboard: http://192.168.99.100:30000

```
#### Add a customer

```curl -H "Content-Type: application/json" -X POST -d '{"name": "Eric Murphy", "city": "San Francisco", "state": "CA", "zipCode": "94105"}' http://192.168.99.100:30545/customer```