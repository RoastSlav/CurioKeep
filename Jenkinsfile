pipeline {
  agent any

  environment {
    MAVEN_HOME = tool name: 'Maven', type: 'hudson.tasks.Maven$MavenInstallation'
    JDK_HOME   = tool name: 'JDK21', type: 'hudson.model.JDK'
    PATH = "${JDK_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"

    IMAGE = "roastslav/curiokeep"
  }

  stages {

    stage('PR: Build & Test') {
      when { changeRequest() }
      steps {
        sh 'java -version'
        sh 'mvn -v'
        sh 'mvn -B clean verify'
      }
    }

    stage('Main: Build Jar (no tests)') {
      when { allOf { not { changeRequest() }; branch 'master' } } // or 'main' if you rename
      steps {
        sh 'java -version'
        sh 'mvn -v'
        sh 'mvn -B -DskipTests -Pfrontend clean package'
      }
    }

    stage('Main: Docker Build') {
      when { allOf { not { changeRequest() }; branch 'master' } }
      steps {
        script {
          env.IMAGE_TAG = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        }
        sh 'docker build -t $IMAGE:$IMAGE_TAG -t $IMAGE:latest .'
      }
    }

    stage('Main: Docker Push') {
      when { allOf { not { changeRequest() }; branch 'master' } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
          sh '''
            echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
            docker push $IMAGE:$IMAGE_TAG
            docker push $IMAGE:latest
            docker logout || true
          '''
        }
      }
    }
  }
}
