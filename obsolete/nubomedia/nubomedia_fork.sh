#!/bin/bash

[ -z $KURENTO_PASSWD -o -z $KURENTO_USER ] && (echo "Need a passwd for jenkinskurento" ; exit 1)

REPOSITORIES=$(cat nubomedia-repositories-fork)
for repo in $REPOSITORIES
do
  echo "Forking repo $repo"

  organization=$(echo $repo | cut -d '/' -f 1)
  reponame=$(echo $repo | cut -d '/' -f 2)
  echo "Org: $organization, Repo: $reponame"

  curl -i -u "$KURENTO_USER:$KURENTO_PASSWD" -d '{"organization":"nubomedia"}' -X POST https://api.github.com/repos/$organization/$reponame/forks
  if [ $? != 0 ]; then
    echo "Failed command for repo $repo" >> fork-output.log

  fi
done
