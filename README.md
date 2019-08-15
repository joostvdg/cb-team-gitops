# CloudBees Core Teams GitOps

!!!! Notes

* GKE doesn't allow serviceaccounts to assign RBAC unless they are cluster admin: we don't want this
    * https://cloud.google.com/kubernetes-engine/docs/how-to/role-based-access-control

## Premise

## Team Recipe

## Teams

## Create Team in Alternative Namespace

### Create Helm chart repo via GitHub

https://github.com/kmzfs/helm-repo-in-github

### Setup OPS Team

```bash
export OC_URL=https://cbcore.aks.kearos.net/cjoc
export USR=jvandergriendt
export TKN=110b194124727c70b26e706da754b05e13
```

```bash
http --download ${OC_URL}/jnlpJars/jenkins-cli.jar --verify false
```

```bash
alias cboc="java -jar jenkins-cli.jar -noKeyAuth -auth ${USR}:${TKN} -s ${OC_URL}"
```

```bash
cboc version
```

```bash
export LDAP_SERVER=jx-ldap:389
export LDAP_MANAGER_PASSWORD=secret
cboc groovy = < ldap-rbac-config.groovy ${LDAP_SERVER} ${LDAP_MANAGER_PASSWORD}
```

```bash
# `POST` to `/scriptText` with a body param `script`.
http POST ${OC_URL}/scriptText 
```

```bash
USR=barbossa
TKN=1151763070b972abdadfa6a2e62779b91b
```

```bash
alias cboc="java -jar jenkins-cli.jar -noKeyAuth -auth ${USR}:${TKN} -s ${OC_URL}"
```

```bash
cboc version
```

```bash
kubectl apply -f cb-ops-namespace.yaml
kubectl apply -f oc-mastermanagement-service-account.yaml -n cb-ops
kubectl apply -f jenkins-agent-config-map.yaml -n cb-ops
cboc groovy = < configure-oc-namespace.groovy cb-ops
cboc teams ops --put < teams-cb-ops.json
sleep 30
k get sts -n cb-ops
cboc groovy = < configure-oc-namespace.groovy jx-production
```

### Update Team Recipes

* make sure you have the `joostvdg` recipe
* make sure the CLI image is up-to-date

#### Create Kaniko secret

See: https://go.cloudbees.com/docs/cloudbees-core/cloud-install-guide/kubernetes-using-kaniko/

```bash
DOCKER_REGISTRY_SERVER=index.docker.io
DOCKERHUB_USR=caladreas
DOCKERHUB_EMAIL=joostvdg@gmail.com
DOCKERHUB_PSW=
```

```bash
kubectl create secret docker-registry docker-credentials --docker-username=${DOCKERHUB_USR} --docker-password="${DOCKERHUB_PSW}" --docker-email=${DOCKERHUB_EMAIL}
```

### Update PodTemplate on CJOC to comply with Quota

* jnlp container cpu (request and limits): `500m`
* jnlp container memory (requests and limits): `512Mi`

### Update Pipelines to comply with Quota

```yaml
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1024Mi"
      cpu: "500m"
```

### Configure Team Job

#### Credentials

```bash
OPS_URL=https://cbcore.aks.kearos.net/teams-ops/
```

```bash
alias cbops="java -jar jenkins-cli.jar -noKeyAuth -auth ${USR}:${TKN} -s ${OPS_URL}"
```

```bash
cbops version
```

```bash
SECRET=
```

```bash
sed s/XXXXXXXXXX/${SECRET}/g jenkins-credential.xml > tmp.xml && \
    cbops import-credentials-as-xml "folder::item::/ops" < tmp.xml && \
    rm tmp.xml && \
    SECRET=""
```

```bash
SECRET=
```

```bash
sed s/XXXXXXXXXX/${SECRET}/g jenkins-credential-jenkins.xml > tmp.xml && \
    cbops import-credentials-as-xml "folder::item::/ops" < tmp.xml && \
    rm tmp.xml && \
    SECRET=""
```

```bash
SECRET=
```

```bash
sed s/XXXXXXXXXX/${SECRET}/g jenkins-credential-token.xml > tmp.xml && \
    cbops import-credentials-as-xml "folder::item::/ops" < tmp.xml && \
    rm tmp.xml && \
    SECRET=""
```


```bash
cbops create-job ops/joostvdg < github-org-job.xml
```

```bash
open ${OPS_URL}/job/ops/job/joostvdg/
```


```bash
open ${OPS_URL}/blue/organizations/ops/pipelines/
```

#### Steps

* create new namespace for ops `cb-ops`
    * namespace `cb-ops`
    * resource quota
* create service account for `cb-ops`
    * role for managing pods
    * role for creating namespaces
    * role bindings for both
* allow Operations Center (OC) to create `cb-ops` team in its own namespace
    * create **Role** for creating masters
    * create **RoleBinding** for `cjoc` service account in OC's namespace
* configure Operations Center to use `cb-ops` namespace
* create new team master (`cb-ops`) in the alternative namespace
* let `cb-ops` master create new namespaces

!!!! Have to set Requests/Limits, else ResourceQuota denies Pod !!!!
!!!! Service Account Invalid? !!!!

### Create Alternative Namespace


### Configure Operations Center to use Alternative Namespace

#### xml config

```xml
<?xml version='1.1' encoding='UTF-8'?>
<com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning_-DescriptorImpl plugin="master-provisioning-kubernetes@2.2.6">
  <clusterEndpoints>
    <com.cloudbees.masterprovisioning.kubernetes.KubernetesClusterEndpoint>
      <id>default</id>
      <name>kubernetes</name>
      <skipTlsVerify>false</skipTlsVerify>
      <namespace>cb-ops</namespace>
    </com.cloudbees.masterprovisioning.kubernetes.KubernetesClusterEndpoint>
  </clusterEndpoints>
  <javaOptions>-XshowSettings:vm -XX:MaxRAMFraction=1 -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication -Dhudson.slaves.NodeProvisioner.initialDelay=0</javaOptions>
  <jenkinsOptions></jenkinsOptions>
  <envVars></envVars>
  <systemProperties></systemProperties>
  <jenkinsUrl>http://cjoc.cloudbees-core.svc.cluster.local/cjoc</jenkinsUrl>
  <disk>50</disk>
  <memory>3072</memory>
  <ratio>0.7</ratio>
  <cpus>1.0</cpus>
  <masterUrlPattern></masterUrlPattern>
  <terminationGracePeriodSeconds>1200</terminationGracePeriodSeconds>
  <livenessInitialDelaySeconds>300</livenessInitialDelaySeconds>
  <livenessPeriodSeconds>10</livenessPeriodSeconds>
  <livenessTimeoutSeconds>10</livenessTimeoutSeconds>
  <fsGroup>1000</fsGroup>
  <nodeSelectors></nodeSelectors>
  <yaml></yaml>
</com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning_-DescriptorImpl>
```

#### Groovy

```groovy
import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning
import com.cloudbees.masterprovisioning.kubernetes.KubernetesClusterEndpoint

println "=== KubernetesMasterProvisioning Configuration - start"

println "== Retrieving main configuration"
def descriptor = Jenkins.getInstance().getInjector().getInstance(KubernetesMasterProvisioning.DescriptorImpl.class)
def namespace = 'cloudbees-core'

// def kubernetesMasterProvisioning.setNamespace(namespace)

def currentKubernetesClusterEndpoint =  descriptor.getClusterEndpoints().get(0)
println "= Found current endpoint"
println "= " + currentKubernetesClusterEndpoint.toString()
def id = currentKubernetesClusterEndpoint.getId()
def name = currentKubernetesClusterEndpoint.getName()
def url = currentKubernetesClusterEndpoint.getUrl()
def credentialsId = currentKubernetesClusterEndpoint.getCredentialsId()

println "== Setting Namspace to " + namespace
def updatedKubernetesClusterEndpoint = new KubernetesClusterEndpoint(id, name, url, credentialsId, namespace)
def clusterEndpoints = new ArrayList<KubernetesClusterEndpoint>()
clusterEndpoints.add(updatedKubernetesClusterEndpoint)
descriptor.setClusterEndpoints(clusterEndpoints)

println "== Saving Jenkins configuration"
descriptor.save()

println "=== KubernetesMasterProvisioning Configuration - finish"
```
