/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.migrate.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.DBBConstants.CopyMode
import com.ibm.dbb.utils.FileUtils
import com.ibm.jzos.*
import groovy.transform.*
import java.io.File
import groovy.cli.commons.*

@Field def CHAR_NL = 0x15
@Field def CHAR_CR = 0x0D
@Field def CHAR_LF = 0x25
@Field def CHAR_SHIFT_IN = 0x0F
@Field def CHAR_SHIFT_OUT = 0x0E
@Field def CHAR_DEL = -1  // For some reason 0xFF doesn't work but using -1 does

/**********************************************************************************
usage: migrate [options] <data sets>|<mapping files>
Use this migration tool to migrate members from data sets to a local GIT
repository on HFS
 -m,--mapping <mapping>         The ID of mapping rule (optional), for
                                example:
                                com.ibm.dbb.migration.SimpleMapping,
                                com.ibm.dbb.migration.HlqMapping
 -o,--output <output>           Output of the generated mapping file
                                (optional)
 -p,--preview                   Perform a dry-run to generate a mapping
                                file (optional)
 -r,--repository <repository>   Local GIT repository to migrate to
                                (required)
 -l,--log <logfile>             Path to a logfile (optional)
 -le,--logEncoding <logEncoding>    Encoding for the logfile (optional) - default set to IBM-1047
 -np, --non-printable <level>   Scan level ("info" or "warning") for
                                non-printable characters (optional)
                                "info" - the files are copied as text (codepage conversion occurs)
                                "warning" - the files are copied as binary
 -t, --error-table              Print a table to visualize non-printable and non-roundtrippable character errors

***********************************************************************************/

def headerMsg = 'Use this migration tool to migrate members from data sets to a local GIT repository on HFS'
cli = new CliBuilder(usage:'migrate [options] <data sets>', header: headerMsg, stopAtNonOption: false)
cli.r(longOpt:'repository', args:1, argName:'repository', 'Local GIT repository to migrate to (required)')
cli.o(longOpt:'output', args:1, argName:'output', 'Output of the generated mapping file (optional)')
cli.m(longOpt:'mapping', args:1, argName:'mapping', 'The ID of mapping rule (optional), for example: com.ibm.dbb.migration.MappingRule')
cli.p(longOpt:'preview', 'Perform a dry-run to generate a mapping file (optional)')
cli.l(longOpt:'log', args:1, argName:'logfile', 'Path to a logfile (optional)')
cli.le(longOpt:'logEncoding', args:1, argName:'logEncoding', defaultValue: 'IBM-1047', 'Encoding for the logfile (optional), default is IBM-1047')
cli.np(longOpt:'non-printable', args:1, argName:'level', 'Scan level ("info" or "warning") for non-printable characters (optional)')
cli.t(longOpt:'error-table', 'Print a table to visualize non-printable and non-roundtrippable character errors')

def parameters = cli.parse(args)
BuildProperties.setProperty("dbb.file.tagging", "true") // Enable dbb file tagging

// Validate that recognized parameters exist
if (parameters == null) {
	cli.usage()
	System.exit(2)
}

// Validate that at least one argument exists (set of datsets or a mapping file)
if (parameters.arguments().size() != 1)
{
	cli.usage()
	System.exit(2)
}
// Validate required parameter
if (!parameters.r)
{
	cli.usage()
	System.exit(2)
}

// Handle output parameter
def writer = null
if (parameters.o)
{
	def outputFile = new File(parameters.o)
	writer = outputFile.newWriter(true)
	printlnLog("Generated mappings will be saved in $outputFile")
}

// Handle log file
@Field def logEncoding = null
@Field def logFilePath = null
@Field def logfileWriter = null
if (parameters.l) {
	def logFile = new File(parameters.l)
	logFilePath = logFile.getPath()
	logEncoding = parameters.le as String
	logfileWriter = logFile.newWriter(logEncoding, true)
	printlnLog("Messages will be saved in '${logFilePath}' with encoding '$logEncoding'")
}

// Handle non-printable flag (-np)
@Field def npLevel = null
if (parameters.np) {
	npLevel = parameters.np
	if (npLevel != "info" && npLevel != "warning") {
		printlnLog(" ! Error: Unrecognized value for non-printable scan level.")
		cli.usage()
		System.exit(2)
	} else {
		printlnLog("Non-printable scan level is " + npLevel)
	}
}

// Handle error-table flag (-t)
@Field def hasErrorTable = false
if (parameters.t)
	hasErrorTable = true

@Field File messageFile = File.createTempFile("migration",null,null) // Create temporary file to hold migration messages
messageFile.deleteOnExit() // Delete file upon JVM termination

def arg = parameters.arguments()[0]
boolean isMappingFileSpecified = isMappingFileSpecified(arg)
def gitAttributePathCache = []

// Handle preview parameter and mapping file
if (parameters.p && isMappingFileSpecified)
{
	printlnLog("Error: Preview flag is not supported when using a mapping file")
	cli.usage()
	System.exit(2)
}
	
// Handle preview parameter 
if (parameters.p)
	printlnLog("Preview flag is specified, no members will be copied to HFS")
def isPreview = parameters.p
		
// Create the repository directory if not exist
def repository = new File(parameters.r)
if (!repository.exists())
	repository.mkdirs()
printlnLog("Local GIT repository: $repository")
def gitAttributeWriter = new File(repository, ".gitattributes").newWriter("ISO8859-1", true)

if (isMappingFileSpecified)
{        
	def mappingFiles = arg.split(",")
	
	mappingFiles.each { path ->
		def mappingFile = new File(path)
		printlnLog("Migrate data sets using mapping file $mappingFile")
		mappingFile.eachLine { line ->
			//Ignore comment which starts with #
			if (!line.trim().startsWith("#"))
			{
				//Each line should be in the form of:  "dataset hfsPath pdsEncoding=Cp1047"
				def lineSegments = line.split(" ")
				if (lineSegments.size() >= 2)
				{
					def datasetMember = lineSegments[0]
					boolean isValidDatasetMember = datasetMember ==~ ".*\\((.*[a-zA-Z\\*].*)\\)"
					if (isValidDatasetMember)
					{
						def datasetMemberSegment = datasetMember.split("[\\(\\)]")
						def dataset = datasetMemberSegment[0]
						def member = datasetMemberSegment[1]
						def hfsPath = lineSegments[1]
						def pdsEncoding = null
						if (lineSegments.size() > 2)
						{
							def pdsEncodingSegments = lineSegments[2].split("=")
							if (pdsEncodingSegments.size() == 2 && pdsEncodingSegments[0] == 'pdsEncoding')
								pdsEncoding = pdsEncodingSegments[1].trim()
						}
						def encodingString = pdsEncoding ?: 'default encoding'
						
						
						def rc = detector(dataset, member)
						if (rc == 4) {
							printlnLog("[INFO] Copying $datasetMemberSegment to $hfsPath using $encodingString")
							printlnLog(" ! Possible migration issue:\n")
							printMessageFile()
							new CopyToHFS().dataset(dataset).member(member).file(new File(hfsPath)).pdsEncoding(pdsEncoding).execute()

						}
						else if (rc == 8) { 
							printlnLog("[WARNING] Copying $datasetMemberSegment to $hfsPath.")
							printlnLog(" ! Possible migration issue:\n")
							printMessageFile()
							printlnLog(" ! Copying using BINARY mode")
							new CopyToHFS().dataset(dataset).member(member).file(new File(hfsPath)).pdsEncoding(pdsEncoding).copyMode(CopyMode.BINARY).execute()
						}
						else if (rc == 12) {
							printlnLog("[ERROR] Error while reading ${mappingInfo.getFullyQualifiedDsn()}:\n")
							printMessageFile()
						}
						else {
							printlnLog("Copying $datasetMemberSegment to $hfsPath using $encodingString")
							new CopyToHFS().dataset(dataset).member(member).file(new File(hfsPath)).pdsEncoding(pdsEncoding).execute()
						}
						if (gitAttributeWriter)
						{
							String gitAttributeLine = null
							if (rc == 8)
								gitAttributeLine = repository.toPath().relativize(new File(hfsPath).toPath()).toFile().path + " binary"
							else if (rc <= 4)
								gitAttributeLine = generateGitAttributeEncodingLine(repository, new File(hfsPath), gitAttributePathCache, pdsEncoding)
							if (gitAttributeLine)
								gitAttributeWriter.writeLine(gitAttributeLine)
						}
					}
				}
			}
		}
	}
}
else
{
	def datasets = arg.split(",")

	//Verify if mapping rule is defined and parse it, otherwise use default 'com.ibm.dbb.migration.SimpleMapping'
	def (mappingRuleId,mappingRuleAttrs) = parameters.m ? parseMappingRule(parameters.m) : ['com.ibm.dbb.migration.MappingRule',null]
	def mappingRule = (mappingRuleId as Class).newInstance(repository,mappingRuleAttrs)
	
	printlnLog("Using mapping rule $mappingRuleId to migrate the data sets")
	
	datasets.each { dataset ->
		printlnLog("Migrating data set $dataset")
		def mappingInfos = mappingRule.generateMapping(dataset)
		mappingInfos.each { mappingInfo ->
			if (isPreview) {
				def encodingString = mappingInfo.pdsEncoding ?: 'default encoding'
				def rc = detector(mappingInfo.dataset, mappingInfo.member)
				if (rc == 4) {
					printlnLog("[INFO] Previewing ${mappingInfo.getFullyQualifiedDsn()}")
					printlnLog(" ! Possible migration issue:")
					printMessageFile()
				}
				else if (rc == 8) {
					printlnLog("[WARNING] Previewing ${mappingInfo.getFullyQualifiedDsn()}")
					printlnLog(" ! Possible migration issue:")
					printlnLog(" ! Will copy using BINARY mode")
					printMessageFile()
				}
				else if (rc == 12) {
					printlnLog("[ERROR] Error while reading ${mappingInfo.getFullyQualifiedDsn()}:")
					printMessageFile()
				}
				else {
					printlnLog("Previewing ${mappingInfo.getFullyQualifiedDsn()}. Using $encodingString.")
				}
			}
			else
			{                    
				def encodingString = mappingInfo.pdsEncoding ?: 'default encoding'
				def rc = detector(mappingInfo.dataset, mappingInfo.member)
				if (rc == 4) {
					printlnLog("[INFO] Copying ${mappingInfo.getFullyQualifiedDsn()} to ${mappingInfo.hfsPath} using $encodingString")
					printlnLog(" ! Possible migration issue:")
					printMessageFile()
					new CopyToHFS().dataset(mappingInfo.dataset).member(mappingInfo.member).file(new File(mappingInfo.hfsPath)).pdsEncoding(mappingInfo.pdsEncoding).execute()
				}
				else if (rc == 8) {
					printlnLog("[WARNING] Copying ${mappingInfo.getFullyQualifiedDsn()} to ${mappingInfo.hfsPath}")
					printlnLog(" ! Possible migration issue:")
					printMessageFile()
					printlnLog(" ! Copying using BINARY mode")
					new CopyToHFS().dataset(mappingInfo.dataset).member(mappingInfo.member).file(new File(mappingInfo.hfsPath)).pdsEncoding(mappingInfo.pdsEncoding).copyMode(CopyMode.BINARY).execute()
				}
				else if (rc == 12) {
					printlnLog("[ERROR] Error while reading ${mappingInfo.getFullyQualifiedDsn()}:")
					printMessageFile()
				}
				else {
					printlnLog("Copying ${mappingInfo.getFullyQualifiedDsn()} to ${mappingInfo.hfsPath} using $encodingString")
					new CopyToHFS().dataset(mappingInfo.dataset).member(mappingInfo.member).file(new File(mappingInfo.hfsPath)).pdsEncoding(mappingInfo.pdsEncoding).execute()
				}
				if (gitAttributeWriter)
				{
					def hfsFile = new File(mappingInfo.hfsPath)
					String gitAttributeLine = null
					if (rc == 8)
						gitAttributeLine = repository.toPath().relativize(hfsFile.toPath()).toFile().path + " binary"
					else if (rc <= 4)
						gitAttributeLine = generateGitAttributeEncodingLine(repository, new File(mappingInfo.hfsPath), gitAttributePathCache, mappingInfo.pdsEncoding)
					if (gitAttributeLine)
						gitAttributeWriter.writeLine(gitAttributeLine)
				}         
			}    
			if (writer)
				writer.writeLine(mappingInfo.toString())
		}
	}
}

writer?.close()
gitAttributeWriter?.close()

// Tag .gitattributes file
if (!FileUtils.setFileTag("$repository/.gitattributes", "ISO8859-1"))
	print("*! Error tagging .gitattributes file, must be done manually")

logfileWriter?.close()
if (logFilePath && !FileUtils.setFileTag(logFilePath, logEncoding)) 
	print("*! Error while tagging file '$logFilePath' with '$logEncoding' encoding, must be done manually.")


// Method definitions

def parseMappingRule(String mappingRuleId)
{  
	def mappingIds = ['MappingRule':'com.ibm.dbb.migration.MappingRule', 'com.ibm.dbb.migration.MappingRule':'com.ibm.dbb.migration.MappingRule']
	def temp = mappingRuleId.split("[\\[\\]]")
	if (temp.length == 1)
	{
		return [temp[0], [:]]
	}
	else if (temp.length == 2)
	{
		def id = temp[0]
		id = mappingIds[id]
		def str = temp[1]
		def attrMap = [:]
		def attrStrs = str.split(",")
		attrStrs.each { attrStr ->
			def attr = attrStr.split(":")
			def attrName = attr[0]
			def attrValue = attr[1]
			if (attrValue.startsWith("\"") && attrValue.endsWith("\"") && attrValue.length() > 2)
				attrValue = attrValue.substring(1,attrValue.length()-1)
			attrMap.put(attrName, attrValue)
		}
		return [id,attrMap]
	}
}

def generateGitAttributeEncodingLine(File root, File file, List<String> pathCache, String encoding)
{
	if (encoding == null)
		encoding = 'ibm-1047'

	def relPath = root.toPath().relativize(file.parentFile.toPath()).toFile()
	def index = file.name.lastIndexOf(".")
	def fileExtension = (index == -1 || index == (file.name.length() - 1)) ? null : file.name.substring(index+1)
	def extension = "*"
	if (fileExtension != null)
		extension = extension + fileExtension
	def path = relPath.path + '/' + extension
	if (path.startsWith("/"))
		path = extension
	if (pathCache.contains(path))
		return null
	pathCache.add(path)    
	return path + " zos-working-tree-encoding=${encoding} git-encoding=utf-8"     
}

def isMappingFileSpecified(String argString)
{
	def filesExist = true
	argString.split(",").each { path ->
		def file = new File(path)
		if (!file.exists())
			filesExist = false
	}
	return filesExist
}

/**
 * Detect whether a member contains record that contains non-roundtripable characters (line separator or an empty Shift-Out
 * Shift-In) and optionally whether a member contains a record that contains any non-printable characters (below 0x40)
 * @param dataset the data set contains the member to test
 * @param member the member to test
 * @return an array containing return code and error message
 *          Return Code 0 = No Errors round, 4 = [info] non-printable character detected (copy as text w/ codepage conversion),
 *          8 = [warning] non-printable or non-rountripable char detected (binary copy), 12 = other internal error 
 */
def detector(String dataset, String member) {
	emptyMessageFile()
	def fullyQualifiedDsn = constructDatasetForZFileOperation(dataset, member)
	def reader

	try {
		reader = RecordReader.newReader(fullyQualifiedDsn, ZFileConstants.FLAG_DISP_SHR)
		int LRECL = reader.getLrecl()
		byte[] buf = new byte[LRECL]
		int line = 0

		def foundNonRoundtripable = false
		def foundNonPrintable = false

		int numBytesRead = -1
		while ((numBytesRead = reader.read(buf)) >= 0) {
			line++
			def prevIndex = -1
			def NRmsg = new StringBuilder()
			def NPmsg = new StringBuilder()
			def errorMessage = new StringBuilder()
			def countNR = 0		
			def countNP = 0
			def List<int> npColumnList = new ArrayList<>()
			def List<int> nrColumnList = new ArrayList<>()

			/* Find NL, CR, LF, empty SO&SI, optionally non-printable chars in a record */
			for (int i = 0; i < (numBytesRead); i++) {
				/* Check for non-roundtrippable line seperators: NL, CR, LF */
				if (buf[i] == CHAR_NL || buf[i] == CHAR_CR || buf[i] == CHAR_LF || buf[i] == CHAR_DEL) {
					NRmsg.append(sprintf("        Char 0x%02X at column %d\n", buf[i], i + 1))
					nrColumnList.add(i + 1)
					countNR++
				}
				/* Check for non-roundtrippable empty Shift Out and Shift In */
				else if (buf[i] == CHAR_SHIFT_OUT) { // Find SHIFT_OUT first
					prevIndex = i 
				}
				else if (buf[i] == CHAR_SHIFT_IN && prevIndex != -1 && prevIndex == (i-1)) { // Find SHIFT_IN immediately following SHIFT_OUT
					NRmsg.append(sprintf("        Empty Shift Out and Shift In at column %d\n", prevIndex + 1))
					nrColumnList.add(prevIndex + 1)
			 		countNR++
				}			
				/* Check for other non-printable chars in a record (less than 0x40) */
				else if ((npLevel != null) && (java.lang.Integer.compareUnsigned(0x40, buf[i]) > 0 )) {
					NPmsg.append(sprintf("        Char 0x%02X at column %d\n", buf[i], i + 1))
					npColumnList.add(i + 1)
					countNP++ 
				}
			}

			if (countNR >= 1) {
				errorMessage.append("      Line $line contains non-roundtripable characters:\n" + NRmsg)
				foundNonRoundtripable = true
				printTable(buf, errorMessage, LRECL, nrColumnList)
			}

			if (countNP >= 1) {
				errorMessage.append("      Line $line contains non-printable characters:\n" + NPmsg)
				foundNonPrintable = true
				printTable(buf, errorMessage, LRECL, npColumnList)
			}

			if (errorMessage != "") {
				/* At end of each line, append any error messages to the temporary messageFile */
				messageFile.append(errorMessage)
			}

		}
		if (foundNonRoundtripable)
			return 8
		if (foundNonPrintable) {
			if (npLevel == "info")
				return 4
			if (npLevel == "warning")
				return 8
		} 
	}
	catch (IOException e) {
		// Print IOExceptino to temporary file
		messageFile.append(e.getMessage())
		return 12
	}
	finally {
		if (reader)
			reader.close()
	}
	return 0
}

/**
 * All zFile operations require dataset and member in certain format
 * @param dataset the data set
 * @param member the member in the data set
 * @return a formatted string contains the data set and member for zFile
 * operation
 */
def constructDatasetForZFileOperation(String dataset, String member)
{
	return "//'${dataset}($member)'"
}

/**
 * Print provided message to log file (if log parameter is provided)
 * as well as print the message to standard output. 
 * @param message the message
 */
def printlnLog(String message) {
	if (logfileWriter != null) {
		logfileWriter.writeLine(message)
	}
	println(message)
}

/**
 * Print contents of the temporary error message file
 * to the console as well as the log file if specified.
 */
def printMessageFile() {
	messageFile.eachLine { line -> 
		printlnLog(line) 
	}
}

/**
 * Empty the temporary error message file
 */
def emptyMessageFile() {
	PrintWriter writer = new PrintWriter(messageFile);
	writer.print("")
	writer.close()
}

/**
 * Print a table to visualize errors in records
 * with non-printable and non-roundtrippable characters.
 * @param buf The buffer containing the record to be printed.
 * @param errorMessage The StringBuilder for error messages to which this table will be appended.
 * @param lrecl The length of the record.
 * @param columns The list of columns containing bad characters.
 */
def printTable(byte[] buf, StringBuilder errorMessage, int lrecl, List columns) {
	if (!hasErrorTable)
		return

	String line1 = ""
	String line2
	String line3 = ""
	String line4 = ""
	String line5 = ""

	// Generate header
	for (int i = 1; i <= lrecl; i++) {
		String s;
		if ((i % 10) == 0) {
			s = (i / 10) % 10
		}
		else if ((i % 5) == 0 )
			s = "+"
		else
			s = "-"
		line1 += s
	}
	line1 += "\n"

	// Print line, replace bad characters with spaces
	StringBuilder line2Builder = new StringBuilder(new String(buf))
	for (int column : columns) {
		line2Builder.setCharAt(column - 1, (char) " ")
	}
	line2 = line2Builder.toString()
	line2 += "\n"

	// Generate hex lines
	for (byte b : buf) {
		String hex = String.format("%02X", b)
		if (hex.length() == 1)
			hex = "0" + hex
		if (hex.length() != 2)
			"ERROR hex length is not two."
		line3 += hex.substring(0,1)
		line4 += hex.substring(1)
	}
	line3 += "\n"
	line4 += "\n"

	// Generate indicators over bad characters
	for (int i = 1; i <= lrecl; i++) {
		if (columns.contains(i))
			line5 += "^"
		else
			line5 += " "
	}
	line5 += "\n"

	errorMessage.append(line1 + line2 + line3 + line4 + line5)
}
