pipeline {
  agent any

  environment {
    MAVEN_HOME = tool name: 'Maven', type: 'hudson.tasks.Maven$MavenInstallation'
    JDK_HOME   = tool name: 'JDK21', type: 'hudson.model.JDK'
    PATH = "${JDK_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"

    IMAGE = "roastslav/curiokeep"
    DOCKER_CREDS = credentials('dockerhub-creds')
  }

  stages {

    stage('PR: Build & Test') {
      when { changeRequest() } // ONLY PR builds
      steps {
        sh 'java -version'
        sh 'mvn -v'
        sh 'mvn -B clean verify'
      }
    }

    stage('Main: Build Jar (no tests)') {
      when { not { changeRequest() } } // NOT PR builds (main, tags, manual)
      steps {
        sh 'java -version'
        sh 'mvn -v'
        sh 'mvn -B -DskipTests -Pfrontend clean package'
      }
    }

    stage('Main: Docker Build') {
      when { not { changeRequest() } }
      steps {
        script {
          def tag = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
          env.IMAGE_TAG = tag
        }
        sh 'docker build -t $IMAGE:$IMAGE_TAG -t $IMAGE:latest .'
      }
    }

    stage('Main: Docker Push') {
      when { not { changeRequest() } }
      steps {
        sh '''
          echo "$DOCKER_CREDS_PSW" | docker login -u "$DOCKER_CREDS_USR" --password-stdin
          docker push $IMAGE:$IMAGE_TAG
          docker push $IMAGE:latest
        '''
      }
    }
  }

  post {
    always {
      sh 'docker logout || true'
    }
  }
}