@NonCPS
def getChangeString() {
  MAX_MSG_LEN = 100
  def changeString = ""
  echo "Gathering SCM changes"
  def changeLogSets = currentBuild.changeSets
  for (int i = 0; i < changeLogSets.size(); i++) {
    def entries = changeLogSets[i].items
    for (int j = 0; j < entries.length; j++) {
      def entry = entries[j]
      truncated_msg = entry.msg.take(MAX_MSG_LEN)
      changeString += " - ${truncated_msg} [${entry.author}]\n"
    }
  }

  if (!changeString) {
    changeString = " - No new changes"
  }
  return changeString
}
// Changes func
properties([
    buildDiscarder(logRotator(daysToKeepStr: '', numToKeepStr: '7')),
])
timestamps {
  node {
    try {
      stage('Checkout SCM') {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "${env.BRANCH_NAME}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[credentialsId: 'bc074014-bab1-4fb0-b5a4-4cfa9ded5e66',url: 'git@bitbucket.org:citeck/ecos-community.git']]
        ])
      }
      def project_version = readMavenPom().getVersion()
      mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "qa-cicd", color: 'good', message: " :arrow_forward: **Build project ecos-community:**\n**Branch:** ${env.BRANCH_NAME}\n**Version:** ${project_version}\n**Build id:** ${env.BUILD_NUMBER}\n**Build url:** ${env.BUILD_URL}\n**Changes:**\n" + getChangeString()
      if ((env.BRANCH_NAME != "master") && (!project_version.contains('SNAPSHOT')))  {
        echo "Assembly of release artifacts is allowed only from the master branch!"
        currentBuild.result = 'SUCCESS'
        return
      }
      stage('Assembling and publishing project artifacts') {
        withMaven(mavenLocalRepo: '/opt/jenkins/.m2/repository', tempBinDir: '') {
          sh "mvn clean deploy -Penterprise -DskipTests=true"
          sh "cd war-solution/ && mvn clean deploy -Pjavamelody -DskipTests=true"
        }
      }
      stage('Building an ecos docker image') {
        build job: 'build_ecs_image', parameters: [
          string(name: 'DOCKER_BUILD_DIR', value: '/docker/centos/ecs'),
          string(name: 'ECOS', value: 'community'),
          string(name: 'ECOS_VERSION', value: "${project_version}"),
          string(name: 'ECOS_CLASSIFIER', value: '5.1.f-com'),
          string(name: 'FLOWABLE_VERSION', value: '2.0.5')
        ]
      }
    }
    catch (Exception e) {
      currentBuild.result = 'FAILURE'
      error_message = e.getMessage()
      echo error_message
    }
    script{
      if(currentBuild.result != 'FAILURE'){
        mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "qa-cicd", color: 'good', message: " :white_check_mark: **Build project with ID ${env.BUILD_NUMBER} complete!**"
      }
      else{
        mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "qa-cicd", color: 'danger', message: " @channel :exclamation: **Build project with ID ${env.BUILD_NUMBER} failure with message:**\n```${error_message}```"
      }
    }
  }
}
