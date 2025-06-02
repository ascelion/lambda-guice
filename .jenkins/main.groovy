pipeline {
	agent {
		label 'jdk21'
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
