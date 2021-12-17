properties([
    buildDiscarder(logRotator(daysToKeepStr: '', numToKeepStr: '3')),
])
timestamps {
  node {

    def repoUrl = "git@bitbucket.org:citeck/ecos-community.git"

    stage('Checkout Script Tools SCM') {
      dir('jenkins-script-tools') {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "script-tools"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[credentialsId: 'awx.integrations', url: 'git@bitbucket.org:citeck/pipelines.git']]
        ])
      }
    }
    currentBuild.changeSets.clear()
    def buildTools = load "jenkins-script-tools/scripts/build-tools.groovy"

    try {

      stage('Checkout SCM') {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "${env.BRANCH_NAME}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[credentialsId: 'awx.integrations', url: repoUrl]]
        ])
      }

      def project_version = readMavenPom().getVersion()

      if ((env.BRANCH_NAME != "master") && (env.BRANCH_NAME != "master3") && (!project_version.contains('SNAPSHOT'))) {
        echo "Assembly of release artifacts is allowed only from the master branch!"
        currentBuild.result = 'SUCCESS'
        return
      }

      buildTools.notifyBuildStarted(repoUrl, project_version, env)
      // build-info
      def buildData = buildTools.getBuildInfo(repoUrl, "${env.BRANCH_NAME}", project_version)
      dir('build/build-info') {
        buildTools.writeBuildInfoToFiles(buildData)
      }
      // /build-info

      stage('Assembling and publishing project artifacts') {
        withMaven(mavenLocalRepo: '/opt/jenkins/.m2/repository', tempBinDir: '') {
          sh "mvn enforcer:enforce"
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
          string(name: 'FLOWABLE_VERSION', value: '3.0.5.6')
        ]
      }
      stage('Clean') {
        withMaven(mavenLocalRepo: '/opt/jenkins/.m2/repository', tempBinDir: '') {
          sh "mvn clean"
        }
      }
    }
    catch (Exception e) {
      currentBuild.result = 'FAILURE'
      error_message = e.getMessage()
      echo error_message
    }
    script {
      if (currentBuild.result != 'FAILURE') {
        buildTools.notifyBuildSuccess(repoUrl, env)
      } else {
        buildTools.notifyBuildFailed(repoUrl, error_message, env)
      }
    }
  }
}
