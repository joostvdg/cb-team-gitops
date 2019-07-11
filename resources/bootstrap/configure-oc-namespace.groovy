import hudson.*
import hudson.util.Secret;
import hudson.util.Scrambler;
import hudson.util.FormValidation;
import jenkins.*
import jenkins.model.*
import hudson.security.*

import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning
import com.cloudbees.masterprovisioning.kubernetes.KubernetesClusterEndpoint

println "=== KubernetesMasterProvisioning Configuration - start"

println "== Retrieving main configuration"
def descriptor = Jenkins.getInstance().getInjector().getInstance(KubernetesMasterProvisioning.DescriptorImpl.class)
def namespace = this.args[0]

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