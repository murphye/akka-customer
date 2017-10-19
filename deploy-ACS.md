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

```az acs create --orchestrator-type kubernetes --resource-group customer --name lagom-customer --agent-vm-size Standard_DS1_v2 --agent-count 2```

```
{
  "id": "/subscriptions/e8b1d29a-7336-49c8-9add-f9336020a5d1/resourceGroups/customer/providers/Microsoft.Resources/deployments/azurecli1508210824.1328449780",
  "name": "azurecli1508210824.1328449780",
  "properties": {
    "correlationId": "30dd06e7-e035-4d9d-8fa6-fbda0c6b54fb",
    "debugSetting": null,
    "dependencies": [],
    "mode": "Incremental",
    "outputs": {
      "masterFQDN": {
        "type": "String",
        "value": "lagom-cust-customer-e8b1d2mgmt.eastus.cloudapp.azure.com"
      },
      "sshMaster0": {
        "type": "String",
        "value": "ssh azureuser@lagom-cust-customer-e8b1d2mgmt.eastus.cloudapp.azure.com -A -p 22"
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
    "timestamp": "2017-10-17T03:34:29.656097+00:00"
  },
  "resourceGroup": "customer"
}
```

### Get Credentials (for kubectl)

```az acs kubernetes get-credentials --resource-group=customer --name=lagom-customer```

### Create Container Registry

```az acr create --name customerRegistry --resource-group customer --sku Basic```

```
{
  "adminUserEnabled": false,
  "creationDate": "2017-10-17T03:38:31.456769+00:00",
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
The push refers to a repository [customerregistry.azurecr.io/lightbend/customer-impl]
3f6156a2aad4: Pushed 
762429e05518: Pushed 
2be465c0fdf6: Pushed 
5bef08742407: Pushed 
latest: digest: sha256:9e93455164cb837a36503ec2f469d3b314c90405936352ea006269b9f4f54f1d size: 1159
****************************
***  Deploying customer   ***
****************************
service "customerservice-akka-remoting" created
service "customerservice" created
statefulset "customerservice" created
waiting....................
NAME                                          READY     STATUS    RESTARTS   AGE
po/cassandra-0                                1/1       Running   0          16m
po/customerservice-0                          1/1       Running   0          27s
po/nginx-default-backend-1866436208-vt04q     1/1       Running   0          15m
po/nginx-ingress-controller-667491271-5060h   1/1       Running   0          15m

NAME                                CLUSTER-IP     EXTERNAL-IP     PORT(S)                      AGE
svc/cassandra                       10.0.160.69    <none>          9042/TCP                     16m
svc/customerservice                 None           <none>          9000/TCP                     28s
svc/customerservice-akka-remoting   10.0.211.244   <none>          2551/TCP                     28s
svc/kubernetes                      10.0.0.1       <none>          443/TCP                      21m
svc/nginx-default-backend           10.0.54.68     <none>          80/TCP                       15m
svc/nginx-ingress                   10.0.87.128    13.90.212.201   80:31213/TCP,443:30481/TCP   15m

NAME                           DESIRED   CURRENT   AGE
statefulsets/cassandra         1         1         16m
statefulsets/customerservice   1         1         27s

NAME                              DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
deploy/nginx-default-backend      1         1         1            1           15m
deploy/nginx-ingress-controller   1         1         1            1           15m

NAME                                    DESIRED   CURRENT   READY     AGE
rs/nginx-default-backend-1866436208     1         1         1         15m
rs/nginx-ingress-controller-667491271   1         1         1         15m
```

#### Add a customer

```curl -H "Content-Type: application/json" -X POST -d '{"name": "Eric Murphy", "city": "San Francisco", "state": "CA", "zipCode": "94105"}' http://13.90.212.201/customer```

### Delete Kubernetes Cluster

```az acs delete --resource-group=customer --name=lagom-customer```

### Delete Container Registry

```az acr delete --name customerRegistry --resource-group customer```

### Delete Resource Group

```az group delete --name customer```