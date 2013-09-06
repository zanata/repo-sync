zanata-sync-script
==================

Configurable script to do simple synchronization between a Zanata instance and a code repository.

Pre-requisites
--------------

Groovy (version 1.8+) must be installed and available to run the script.
Java (version 1.6+) must be installed.

Usage
-----

To synchronize Zanata translations into your git code repository.

`sh
groovy ZanataSync zanata-to-git --config config.json
`

To synchronize source files in a Git code repository with Zanata.

`sh
grrovy ZanataSync git-to-zanata --config config.json
`
