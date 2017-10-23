## Running Akka Cluster in Azure Container Service (ACS)

### Recommended Reading

* [Azure Free Trial](https://azure.microsoft.com/en-us/offers/ms-azr-0044p/)
* [Azure Pay-As-You-Go](https://azure.microsoft.com/en-us/offers/ms-azr-0003p/)
* [Azure Container Service Kubernetes](https://docs.microsoft.com/en-us/azure/container-service/kubernetes/container-service-kubernetes-walkthrough)
* [Azure Container Registry](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli)
* [Azure CLI Github Issues](https://github.com/Azure/azure-cli/issues)
* [Deploying clustered Akka applications on Kubernetes](https://developer.lightbend.com/guides/akka-cluster-kubernetes-k8s-deploy/)

### Recommendations

* Do a deployment to Minikube first to learn how the install script works
* Upgrade to Pay-As-You-Go subscription, as the Free Trial will place very limited quotas that will make it difficult to impossible to spin up k8s clusters
* Azure CLI seems to be very raw in regards to the Container Service. If you have problems, look at the Github issues
* Don't rely on the Azure Web Console for much of anything; stick to the CLI

### Install the Azure CLI

```brew install azure-cli```

### Login via the Azure CLI

After creating your Azure account, you will be able to log in through the CLI and the web browser.

```
az login
To sign in, use a web browser to open the page https://aka.ms/devicelogin and enter the code
```

### Create Resource Group

```az group create --name customer --location eastus```

```
{
  "id": "/subscriptions/e8b1d29a-7336-49c8-9add-f9336020a5d1/resourceGroups/customer",
  "location": "eastus",
  "managedBy": null,
  "name": "customer",
  "properties": {
    "provisioningState": "Succeeded"
  },
  "tags": null
}
```

### Create Kubernetes Cluster

```az acs create --orchestrator-type kubernetes --resource-group customer --name akka-customer --agent-vm-size Standard_DS1_v2 --agent-count 2```

```
{
  "id": "/subscriptions/e8b1d29a-7336-49c8-9add-f9336020a5d1/resourceGroups/customer/providers/Microsoft.Resources/deployments/azurecli1508717411.132882874053",
  "name": "azurecli1508717411.132882874053",
  "properties": {
    "correlationId": "44974fc5-1134-4038-9827-08d0d6509e4d",
    "debugSetting": null,
    "dependencies": [],
    "mode": "Incremental",
    "outputs": {
      "masterFQDN": {
        "type": "String",
        "value": "akka-custo-customer-e8b1d2mgmt.eastus.cloudapp.azure.com"
      },
      "sshMaster0": {
        "type": "String",
        "value": "ssh azureuser@akka-custo-customer-e8b1d2mgmt.eastus.cloudapp.azure.com -A -p 22"
      }
    },
    "parameters": {
      "clientSecret": {
        "type": "SecureString"
      }
    },
    "parametersLink": null,
    "providers": [
      {
        "id": null,
        "namespace": "Microsoft.ContainerService",
        "registrationState": null,
        "resourceTypes": [
          {
            "aliases": null,
            "apiVersions": null,
            "locations": [
              "eastus"
            ],
            "properties": null,
            "resourceType": "containerServices"
          }
        ]
      }
    ],
    "provisioningState": "Succeeded",
    "template": null,
    "templateLink": null,
    "timestamp": "2017-10-23T00:18:47.228638+00:00"
  },
  "resourceGroup": "customer"
}
```

### Get Credentials (for kubectl)

```az acs kubernetes get-credentials --resource-group=customer --name=akka-customer```

### Create Container Registry

```az acr create --name customerRegistry --resource-group customer --sku Basic```

```
{
  "adminUserEnabled": false,
  "creationDate": "2017-10-23T00:20:07.281827+00:00",
  "id": "/subscriptions/e8b1d29a-7336-49c8-9add-f9336020a5d1/resourceGroups/customer/providers/Microsoft.ContainerRegistry/registries/customerRegistry",
  "location": "eastus",
  "loginServer": "customerregistry.azurecr.io",
  "name": "customerRegistry",
  "provisioningState": "Succeeded",
  "resourceGroup": "customer",
  "sku": {
    "name": "Basic",
    "tier": "Basic"
  },
  "status": null,
  "storageAccount": null,
  "tags": {},
  "type": "Microsoft.ContainerRegistry/registries"
}
```

### Login to Container Registry

```az acr login --name customerRegistry```

```
Login Succeeded
```

### Deploy Akka Cluster to Kubernetes

```deploy/kubernetes/scripts/install --registry customerregistry.azurecr.io --all```

#### Output:

```
****************************
***  Uploading customer   ***
****************************
The push refers to a repository [customerregistry.azurecr.io/lightbend/akka-customer]
aa70fa8d45c3: Pushed 
1e14c24a1e00: Pushed 
30688d580ca7: Pushed 
9b790cee2175: Pushed 
348667646bbd: Pushed 
1d381f39fece: Pushed 
e6683fd09097: Pushed 
ffe8422597ee: Pushed 
c2dca236d8e6: Pushed 
d4417cb76edb: Pushed 
0dc1ec77adb3: Pushed 
a75caa09eb1f: Pushed 
latest: digest: sha256:0171f2371a6335597195748dcea02cf18a29552f6a98035461c8c2192435d7e0 size: 2842
****************************
***  Deploying customer   ***
****************************
service "customerservice-akka-remoting" created
service "customerservice" created
statefulset "customerservice" created
waiting......................................................................................................
****************************
***  Deploying nginx     ***
****************************
ingress "customer-ingress" created
deployment "nginx-default-backend" created
service "nginx-default-backend" created
deployment "nginx-ingress-controller" created
service "nginx-ingress" created
waiting................
NAME                                          READY     STATUS    RESTARTS   AGE
po/cassandra-0                                1/1       Running   0          8m
po/customerservice-0                          1/1       Running   0          2m
po/customerservice-1                          1/1       Running   0          1m
po/customerservice-2                          1/1       Running   0          26s
po/nginx-default-backend-1866436208-0vhzb     1/1       Running   0          23s
po/nginx-ingress-controller-667491271-7xbdc   1/1       Running   0          23s

NAME                                CLUSTER-IP     EXTERNAL-IP   PORT(S)                      AGE
svc/cassandra                       10.0.237.122   <none>        9042/TCP                     8m
svc/customerservice                 None           <none>        9000/TCP                     2m
svc/customerservice-akka-remoting   10.0.27.39     <none>        2551/TCP                     2m
svc/kubernetes                      10.0.0.1       <none>        443/TCP                      9m
svc/nginx-default-backend           10.0.132.20    <none>        80/TCP                       23s
svc/nginx-ingress                   10.0.199.222   13.90.151.182 80:30931/TCP,443:32009/TCP   22s

NAME                           DESIRED   CURRENT   AGE
statefulsets/cassandra         1         1         8m
statefulsets/customerservice   3         3         2m

NAME                              DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
deploy/nginx-default-backend      1         1         1            1           23s
deploy/nginx-ingress-controller   1         1         1            1           23s

NAME                                    DESIRED   CURRENT   READY     AGE
rs/nginx-default-backend-1866436208     1         1         1         23s
rs/nginx-ingress-controller-667491271   1         1         1         23s
```

#### Add a customer

```curl -H "Content-Type: application/json" -X POST -d '{"name": "Eric Murphy", "city": "San Francisco", "state": "CA", "zipCode": "94105"}' http://13.90.151.182/customer```

### Delete Kubernetes Cluster

```az acs delete --resource-group=customer --name=akka-customer```

### Delete Container Registry

```az acr delete --name customerRegistry --resource-group customer```

### Delete Resource Group

```az group delete --name customer```