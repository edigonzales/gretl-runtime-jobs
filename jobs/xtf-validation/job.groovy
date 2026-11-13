import java.nio.file.Files
import java.nio.file.StandardCopyOption

parameter('inputFile', type: 'path', required: true, description: 'XTF input')
parameter('model', defaultValue: 'GretlValidationExample', description: 'Local example model')
parameter('validationFile', type: 'path', required: true, description: 'Validation report')

task('validate', IliValidator) {
    if (parameter('model') != 'GretlValidationExample') throw new IllegalArgumentException('Unknown model')
    dataFile = parameter('inputFile')
    modelDirectory = file('models')
}

action('saveReport', dependsOn: 'validate') {
    Files.copy(result('validate').reportFile, parameter('validationFile'), StandardCopyOption.REPLACE_EXISTING)
}

defaults 'saveReport'
