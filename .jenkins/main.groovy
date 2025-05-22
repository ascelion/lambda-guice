pipeline {
	agent {
		label 'agent-jdk17'
	}
	environment {
		PATH = "${WORKSPACE}:${PATH}"
	}
	
	stages {
		stage 'Build & Test', {
			steps {
				sh 'gradlew compileAll'
			}
		}
	}
}
