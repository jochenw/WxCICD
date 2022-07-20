# WxCICD

A CI/CD solution for the webMethods suite, based on Git, Jenkins, Docker, and the webMethods IS.



## Architecture



WxCICD is as a set of components, all of which can be viewed as a separate entity:



```
+----------------+           +--------------+         +-----------+
| Git Repository |  ------>  | CI/CD engine | ------> | Builder   |
+----------------+           +--------------+         +-----------+
                                                           |
           +--------------+       +------------+           |
           | Staging      |<------| Artifact   |<-----------
           | environment  |       | Repository | 
           +--------------+       +------------+
```



### The Git repository

One, or more Git repositories (within this document, we are using "Git" as a synonym for [SCCM system](https://en.wikipedia.org/wiki/Source_Code_Control_System), no matter what the flavour. Subversion, Bitkeeper, or whatever else should work equally well) are hosting the source code for a set of projects. Developers operate by editing these projects.



WxCICD can include an own Git repository, implemented by [Gitea](https://gitea.io/). This is recommended as a lightweight solutionfor hosting projects, that are related to WxCICD itself. On the other hand, external Git repositories, like [Github](https://github.com/), [Gitlab](https://gitlab.com/), [Bitbucket](https://bitbucket.org/), or [Azure DevOps](https://azure.microsoft.com/)), or internally hosted SCCM repositories will work just as well, as long as they are accessible, and usable from the [CI/CD engine](#the-ci-cd-engine).



### The CI/CD engine

The purpose of the CI/CD engine is to run the various CI/CD pipelines, that convert a projects source code into binary artifacts, which can then be deployed to one, or more staging environments.

The CI/CD engine is, where the variousCI/CD pipelines are implemented. Typically, this will be a [Jenkins](https://jenkins.io) server, but other on premise solutions, like [Cruise Control](http://cruisecontrol.sourceforge.net/), [GoCD](https://www.gocd.org/), should be usable, too. Cloud based solutions, like [Azure DevOps](https://azure.microsoft.com/), or [IBM UrbanCode](https://www.ibm.com/cloud/urbancode) can be used, in theory.

That said, WxCICD comes with builtin support for Jenkins, because the author considers this still (as of 2022) to be the de-facto standard.
Therefore, within this document, we will use Jenkins as a synonym for the CI/CD engine.


### The Builder

As of this writing (2022), the only supported possibility for conversion of source code for webMethods projects into binary artifacts in conjunction with deployment of such artifacts is to use the so-called [ABE (Asset Build Environment)](https://documentation.softwareag.com/webmethods/wmsuites/wmsuite10-5/SysReqs_Installation_and_Upgrade/compendium/index.html#page/install-upgrade-webhelp/to-install_products_10.html), and the [Deployer](https://documentation.softwareag.com/webmethods/wmsuites/wmsuite10-1/Deployer/10-1_Deployer_Users_Guide.pdf).

Now, while the ABE is, at least, a command line tool, the Deployer is not. Okay, there are the *projectAutomator* scripts, and the *Deployer*
script. However, the former requires a running Deployer, where it can create a deployment project, and the latter uses the created project within the same, running Deployer.

In other words: If you wish to follow the supported paths, and procedures (who wouldn't?), you need a running webMethods Integration Server, that hosts a ABE, and a Deployer installation. Within WxCICD, we call this the **builder**.

In theory, one might merge the CI/CD engine, and the builder into a single component (for example, by creating a CI/CD engine within IS), but
that would mean a **real lot** of custom code. In practice, it works better to have the CI/CD engine delegate small, well-defined tasks to the builder (like "take that IS packages source code, and create a binary for me), and leave them separated.


### The Artifact Repository

### The staging environment
