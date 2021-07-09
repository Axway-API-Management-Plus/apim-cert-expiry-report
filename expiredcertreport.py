'''

@author: rnatarajan

'''
import sys
import base64

from com.vordel.archive.fed import DeploymentArchive
from com.vordel.es.util import ShorthandKeyFinder
from esapi import EntityStoreAPI
from vtrace import Tracer
from optparse import OptionParser

from java.io import FileWriter
from java.io import  BufferedWriter
from java.util import Calendar
from java.io import ByteArrayInputStream
from java.security.cert import CertificateFactory


log = Tracer(Tracer.INFO)


def deleteExpiredCerts(entityStore):
    fileWriter = None
    bufferedWriter = None
    DOUBLE_QUOTE = "\""
    shkf = ShorthandKeyFinder(entityStore)
    entities = shkf.getEntities('/[Certificates]name=Certificate Store/[Certificate]')
    if entities != None:
        index = 1
        for certEntity in entities:
            content = certEntity.getStringValue('content')
            decodedContent = base64.b64decode(content)
            byteInputStream = ByteArrayInputStream(decodedContent)
            certFactory = CertificateFactory.getInstance("X.509")
            cert = certFactory.generateCertificate(byteInputStream)
            references = entityStore.findReferringEntities(certEntity.getPK())
            expiryDate = cert.getNotAfter()
            dn = certEntity.getStringValue('dname')
            calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, int(daysBeforeExpires))
            currentDate = calendar.getTime()
            if currentDate.after(expiryDate):

                log.info("The Certificate " + dn + "  - " + "Expiry date :" + expiryDate.toString())
                if fileWriter == None:
                    fileWriter = FileWriter("output.csv")
                    bufferedWriter = BufferedWriter(fileWriter)
                    bufferedWriter.write("Index," +   "Expired Certificate DN," +  "Expired Date," + "Policy Reference")
                    bufferedWriter.write("\r\n")
                else:

                    data = str(index) + "," + DOUBLE_QUOTE + dn + DOUBLE_QUOTE +  "," + expiryDate.toString() + ","

                    if references.isEmpty() == False:
                        data += "Yes"
                    else:
                        data += "No"
                    data += "\r\n"
                    index = index + 1
                    bufferedWriter.write(data)

        if bufferedWriter != None:
            bufferedWriter.flush()
            bufferedWriter.close()
        if fileWriter != None:
            fileWriter.close()



if __name__ == '__main__':

    parser = OptionParser(prog="expiredCertReport",
    description="Generate report for Expired certificates from policies")
    parser.add_option('--fedPath', dest='fedPath', help='.fed file path e.g : /home/axway/dev.fed')
    parser.add_option('--policyProjDir', dest='policyProjDir', help='Policy project directory e.g: /home/axway/apiprojects/dev')
    parser.add_option('--daysBeforeExpires', dest='daysBeforeExpires', help='"Days Before Expires e.g: 7')

    (options, args) = parser.parse_args()
    print options
    daysBeforeExpires = getattr(options, 'daysBeforeExpires')
    fedPath = getattr(options, 'fedPath')

    if fedPath != None:
        fedArchive = DeploymentArchive(fedPath)
        entityStore = fedArchive.getEntityStore()
        deleteExpiredCerts(entityStore)
        sys.exit(0)

    policyProjDir = getattr(options, 'policyProjDir')

    if policyProjDir != None:
        configURL = 'federated:file:'+policyProjDir+'configs.xml'
        log.info('Project URL' + configURL)
        try:
            store = EntityStoreAPI.create(configURL, '')
            deleteExpiredCerts(store.es)
            store.es.disconnect()
            sys.exit(0)
        except ValueError, val:
            print 'Problem connecting to project: ', val
            sys.exit(1)

    sys.exit(1)
    pass
