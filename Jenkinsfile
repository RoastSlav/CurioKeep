pipeline {
  agent any

  environment {
    DOCKER_REPO = "roastslav/curiokeep"
    DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
    VERSION_FILE = '.last_version'
    TAG_PREFIX   = 'v'
  }

  stages {

    stage('Build (PR runs tests, master skips tests)') {
      steps {
        withMaven(maven: 'Maven') {
          script {
            if (env.CHANGE_ID) {
              // PR build -> run tests
              sh 'mvn -B clean verify'
            } else {
              // master build -> build full jar incl frontend, skip tests
              sh 'mvn -B -DskipTests -Pfrontend clean package'
            }
          }
        }
      }
    }

    stage('Resolve version & decide tags') {
      // Only needed on non-PR builds
      when { expression { !env.CHANGE_ID } }
      steps {
        withMaven(maven: 'Maven') {
          script {
            env.APP_VERSION = sh(
              returnStdout: true,
              script: "mvn -q -DforceStdout help:evaluate -Dexpression=project.version"
            ).trim()

            def cleaned = env.APP_VERSION.toLowerCase().replaceAll('[^a-z0-9._-]', '')
            if (cleaned != env.APP_VERSION) {
              echo "Normalizing version '${env.APP_VERSION}' -> '${cleaned}' for Docker tag"
            }
            env.APP_VERSION = cleaned

            env.PREV_VERSION    = fileExists(env.VERSION_FILE) ? readFile(env.VERSION_FILE).trim() : ''
            env.VERSION_CHANGED = (env.APP_VERSION != env.PREV_VERSION) ? 'true' : 'false'

            def verTag = (env.TAG_PREFIX?.trim()) ? "${env.TAG_PREFIX}${env.APP_VERSION}" : env.APP_VERSION
            env.IMAGE_LATEST  = "${env.DOCKER_REPO}:latest"
            env.IMAGE_VERSION = "${env.DOCKER_REPO}:${verTag}"

            echo "POM version: ${env.APP_VERSION} | Previous: ${env.PREV_VERSION} | Changed: ${env.VERSION_CHANGED}"
            echo "Tags -> latest: ${env.IMAGE_LATEST} ; version: ${env.IMAGE_VERSION}"
          }
        }
      }
    }

    stage('Docker Build and Push Multi-Arch') {
      when {
        allOf {
          expression { !env.CHANGE_ID }      // not a PR
          branch 'master'                    // only master
          expression { env.VERSION_CHANGED == 'true' }
        }
      }
      steps {
        withCredentials([usernamePassword(
          credentialsId: DOCKER_CREDENTIALS_ID,
          passwordVariable: 'DOCKER_PASS',
          usernameVariable: 'DOCKER_USER'
        )]) {
          sh '''
            set -e
            docker version
            docker buildx version || true
            docker run --privileged --rm tonistiigi/binfmt --install arm64,amd64
            BUILDER_NAME=$(docker buildx create --use || true)
            docker buildx inspect --bootstrap

            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

            docker buildx build \
              --platform linux/amd64,linux/arm64 \
              -t "${IMAGE_LATEST}" \
              -t "${IMAGE_VERSION}" \
              --label "org.opencontainers.image.version=${APP_VERSION}" \
              --push .

            docker logout
            [ -n "$BUILDER_NAME" ] && docker buildx rm "$BUILDER_NAME" || true
          '''
        }
      }
    }

    stage('Skip Docker (version unchanged / PR build)') {
      when {
        anyOf {
          expression { env.CHANGE_ID }                 // PR
          expression { env.VERSION_CHANGED != 'true' } // no version bump
        }
      }
      steps {
        echo "Skipping Docker build & push."
      }
    }

    stage('Persist version & Cleanup') {
      when { expression { !env.CHANGE_ID } } // only non-PR
      steps {
        writeFile file: "${env.VERSION_FILE}", text: env.APP_VERSION + "\n"
        sh "docker system prune -f || true"
      }
    }
  }
}
