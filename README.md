# CloudBees Core Teams GitOps

## Premise

## Team Recipe

## Teams

## Create Team in Alternative Namespace

### Setup OPS Team

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

### Create Alternative Namespace



### Configure Operations Center to use Alternative Namespace

