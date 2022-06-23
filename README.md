# WxCICD

WxCICD is a  CI/CD solution for the webMethods suite, based on Git, Jenkins, Docker, and the webMethofds IS. Basically, it allows you to develop your solutions. Once a Commit is pushed to your repository, the following will happen:

1. Jenkins detects, that a change has been done.
2. Jenkins clones the Git repository to it's local file system.
3. Jenkins validates the code by invoking one, or more tools for static analysis of flow code, like ISCCR, or QSE IS. (Quality Gate 1)
4. Jenkins invokes the Builder (an integrated webMethod IS) to run the ABE (Asset Build Environment). Builder will create a set of artifacts (Zip files with IS packages, and so-called ACDL files), that consitute the output.
5. Jenkins copies the artifacts into a snapshot repository.
6. Jenkins deploys the artifacts into the Builder by invoking the Project Automator, and the Deployer.
7. Jenkins invokes the webMethods test suite to execute the unit tests, that are integrated in the projects repository. (Quality Gate 2)
8. If the tests are successful (all quality gates passed), the snapshot is being deployed to the integration environment, where it can be tested. It is possible to disable that deployment on a per-branch basis. (Typically, you only want the main branch in the integration environment, and not feature branches, or outdated 
9. If the tests are successful, Jenkins approves the build by adding an approval file to the snapshot repository. The approval file constitutes the permission to promote the snapshot build to a release. Releases can be deployed to staging environments.
