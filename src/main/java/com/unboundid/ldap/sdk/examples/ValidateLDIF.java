/*
 * Copyright 2008-2018 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2018 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk.examples;



import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.Version;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldap.sdk.schema.EntryValidator;
import com.unboundid.ldif.DuplicateValueBehavior;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.ldif.LDIFReaderEntryTranslator;
import com.unboundid.ldif.LDIFWriter;
import com.unboundid.util.LDAPCommandLineTool;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;

import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a simple tool that can be used to validate that the
 * contents of an LDIF file are valid.  This includes ensuring that the contents
 * can be parsed as valid LDIF, and it can also ensure that the LDIF content
 * conforms to the server schema.  It will obtain the schema by connecting to
 * the server and retrieving the default schema (i.e., the schema which governs
 * the root DSE).  By default, a thorough set of validation will be performed,
 * but it is possible to disable certain types of validation.
 * <BR><BR>
 * Some of the APIs demonstrated by this example include:
 * <UL>
 *   <LI>Argument Parsing (from the {@code com.unboundid.util.args}
 *       package)</LI>
 *   <LI>LDAP Command-Line Tool (from the {@code com.unboundid.util}
 *       package)</LI>
 *   <LI>LDIF Processing (from the {@code com.unboundid.ldif} package)</LI>
 *   <LI>Schema Parsing (from the {@code com.unboundid.ldap.sdk.schema}
 *       package)</LI>
 * </UL>
 * <BR><BR>
 * Supported arguments include those allowed by the {@link LDAPCommandLineTool}
 * class (to obtain the information to use to connect to the server to read the
 * schema), as well as the following additional arguments:
 * <UL>
 *   <LI>"--schemaDirectory {path}" -- specifies the path to a directory
 *       containing files with schema definitions.  If this argument is
 *       provided, then no attempt will be made to communicate with a directory
 *       server.</LI>
 *   <LI>"-f {path}" or "--ldifFile {path}" -- specifies the path to the LDIF
 *       file to be validated.</LI>
 *   <LI>"-c" or "--isCompressed" -- indicates that the LDIF file is
 *       compressed.</LI>
 *   <LI>"-R {path}" or "--rejectFile {path}" -- specifies the path to the file
 *       to be written with information about all entries that failed
 *       validation.</LI>
 *   <LI>"-t {num}" or "--numThreads {num}" -- specifies the number of
 *       concurrent threads to use when processing the LDIF.  If this is not
 *       provided, then a default of one thread will be used.</LI>
 *   <LI>"--ignoreUndefinedObjectClasses" -- indicates that the validation
 *       process should ignore validation failures due to entries that contain
 *       object classes not defined in the server schema.</LI>
 *   <LI>"--ignoreUndefinedAttributes" -- indicates that the validation process
 *       should ignore validation failures due to entries that contain
 *       attributes not defined in the server schema.</LI>
 *   <LI>"--ignoreMalformedDNs" -- indicates that the validation process should
 *       ignore validation failures due to entries with malformed DNs.</LI>
 *   <LI>"--ignoreMissingRDNValues" -- indicates that the validation process
 *       should ignore validation failures due to entries that contain an RDN
 *       attribute value that is not present in the set of entry
 *       attributes.</LI>
 *   <LI>"--ignoreStructuralObjectClasses" -- indicates that the validation
 *       process should ignore validation failures due to entries that either do
 *       not have a structural object class or that have multiple structural
 *       object classes.</LI>
 *   <LI>"--ignoreProhibitedObjectClasses" -- indicates that the validation
 *       process should ignore validation failures due to entries containing
 *       auxiliary classes that are not allowed by a DIT content rule, or
 *       abstract classes that are not subclassed by an auxiliary or structural
 *       class contained in the entry.</LI>
 *   <LI>"--ignoreProhibitedAttributes" -- indicates that the validation process
 *       should ignore validation failures due to entries including attributes
 *       that are not allowed or are explicitly prohibited by a DIT content
 *       rule.</LI>
 *   <LI>"--ignoreMissingAttributes" -- indicates that the validation process
 *       should ignore validation failures due to entries missing required
 *       attributes.</LI>
 *   <LI>"--ignoreSingleValuedAttributes" -- indicates that the validation
 *       process should ignore validation failures due to single-valued
 *       attributes containing multiple values.</LI>
 *   <LI>"--ignoreAttributeSyntax" -- indicates that the validation process
 *       should ignore validation failures due to attribute values which violate
 *       the associated attribute syntax.</LI>
 *   <LI>"--ignoreSyntaxViolationsForAttribute" -- indicates that the validation
 *       process should ignore validation failures due to attribute values which
 *       violate the associated attribute syntax, but only for the specified
 *       attribute types.</LI>
 *   <LI>"--ignoreNameForms" -- indicates that the validation process should
 *       ignore validation failures due to name form violations (in which the
 *       entry's RDN does not comply with the associated name form).</LI>
 * </UL>
 */
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class ValidateLDIF
       extends LDAPCommandLineTool
       implements LDIFReaderEntryTranslator
{
  /**
   * The end-of-line character for this platform.
   */
  private static final String EOL = System.getProperty("line.separator", "\n");



  // The arguments used by this program.
  private BooleanArgument ignoreDuplicateValues;
  private BooleanArgument ignoreUndefinedObjectClasses;
  private BooleanArgument ignoreUndefinedAttributes;
  private BooleanArgument ignoreMalformedDNs;
  private BooleanArgument ignoreMissingRDNValues;
  private BooleanArgument ignoreMissingSuperiorObjectClasses;
  private BooleanArgument ignoreStructuralObjectClasses;
  private BooleanArgument ignoreProhibitedObjectClasses;
  private BooleanArgument ignoreProhibitedAttributes;
  private BooleanArgument ignoreMissingAttributes;
  private BooleanArgument ignoreSingleValuedAttributes;
  private BooleanArgument ignoreAttributeSyntax;
  private BooleanArgument ignoreNameForms;
  private BooleanArgument isCompressed;
  private FileArgument    schemaDirectory;
  private FileArgument    ldifFile;
  private FileArgument    rejectFile;
  private IntegerArgument numThreads;
  private StringArgument  ignoreSyntaxViolationsForAttribute;

  // The counter used to keep track of the number of entries processed.
  private final AtomicLong entriesProcessed = new AtomicLong(0L);

  // The counter used to keep track of the number of entries that could not be
  // parsed as valid entries.
  private final AtomicLong malformedEntries = new AtomicLong(0L);

  // The entry validator that will be used to validate the entries.
  private EntryValidator entryValidator;

  // The LDIF writer that will be used to write rejected entries.
  private LDIFWriter rejectWriter;



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args  The command line arguments provided to this program.
   */
  public static void main(final String[] args)
  {
    final ResultCode resultCode = main(args, System.out, System.err);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args       The command line arguments provided to this program.
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   *
   * @return  A result code indicating whether the processing was successful.
   */
  public static ResultCode main(final String[] args,
                                final OutputStream outStream,
                                final OutputStream errStream)
  {
    final ValidateLDIF validateLDIF = new ValidateLDIF(outStream, errStream);
    return validateLDIF.runTool(args);
  }



  /**
   * Creates a new instance of this tool.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   */
  public ValidateLDIF(final OutputStream outStream,
                      final OutputStream errStream)
  {
    super(outStream, errStream);
  }



  /**
   * Retrieves the name for this tool.
   *
   * @return  The name for this tool.
   */
  @Override()
  public String getToolName()
  {
    return "validate-ldif";
  }



  /**
   * Retrieves the description for this tool.
   *
   * @return  The description for this tool.
   */
  @Override()
  public String getToolDescription()
  {
    return "Validate the contents of an LDIF file " +
           "against the server schema.";
  }



  /**
   * Retrieves the version string for this tool.
   *
   * @return  The version string for this tool.
   */
  @Override()
  public String getToolVersion()
  {
    return Version.NUMERIC_VERSION_STRING;
  }



  /**
   * Indicates whether this tool should provide support for an interactive mode,
   * in which the tool offers a mode in which the arguments can be provided in
   * a text-driven menu rather than requiring them to be given on the command
   * line.  If interactive mode is supported, it may be invoked using the
   * "--interactive" argument.  Alternately, if interactive mode is supported
   * and {@link #defaultsToInteractiveMode()} returns {@code true}, then
   * interactive mode may be invoked by simply launching the tool without any
   * arguments.
   *
   * @return  {@code true} if this tool supports interactive mode, or
   *          {@code false} if not.
   */
  @Override()
  public boolean supportsInteractiveMode()
  {
    return true;
  }



  /**
   * Indicates whether this tool defaults to launching in interactive mode if
   * the tool is invoked without any command-line arguments.  This will only be
   * used if {@link #supportsInteractiveMode()} returns {@code true}.
   *
   * @return  {@code true} if this tool defaults to using interactive mode if
   *          launched without any command-line arguments, or {@code false} if
   *          not.
   */
  @Override()
  public boolean defaultsToInteractiveMode()
  {
    return true;
  }



  /**
   * Indicates whether this tool should provide arguments for redirecting output
   * to a file.  If this method returns {@code true}, then the tool will offer
   * an "--outputFile" argument that will specify the path to a file to which
   * all standard output and standard error content will be written, and it will
   * also offer a "--teeToStandardOut" argument that can only be used if the
   * "--outputFile" argument is present and will cause all output to be written
   * to both the specified output file and to standard output.
   *
   * @return  {@code true} if this tool should provide arguments for redirecting
   *          output to a file, or {@code false} if not.
   */
  @Override()
  protected boolean supportsOutputFile()
  {
    return true;
  }



  /**
   * Indicates whether this tool should default to interactively prompting for
   * the bind password if a password is required but no argument was provided
   * to indicate how to get the password.
   *
   * @return  {@code true} if this tool should default to interactively
   *          prompting for the bind password, or {@code false} if not.
   */
  @Override()
  protected boolean defaultToPromptForBindPassword()
  {
    return true;
  }



  /**
   * Indicates whether this tool supports the use of a properties file for
   * specifying default values for arguments that aren't specified on the
   * command line.
   *
   * @return  {@code true} if this tool supports the use of a properties file
   *          for specifying default values for arguments that aren't specified
   *          on the command line, or {@code false} if not.
   */
  @Override()
  public boolean supportsPropertiesFile()
  {
    return true;
  }



  /**
   * Indicates whether the LDAP-specific arguments should include alternate
   * versions of all long identifiers that consist of multiple words so that
   * they are available in both camelCase and dash-separated versions.
   *
   * @return  {@code true} if this tool should provide multiple versions of
   *          long identifiers for LDAP-specific arguments, or {@code false} if
   *          not.
   */
  @Override()
  protected boolean includeAlternateLongIdentifiers()
  {
    return true;
  }



  /**
   * Adds the arguments used by this program that aren't already provided by the
   * generic {@code LDAPCommandLineTool} framework.
   *
   * @param  parser  The argument parser to which the arguments should be added.
   *
   * @throws  ArgumentException  If a problem occurs while adding the arguments.
   */
  @Override()
  public void addNonLDAPArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    String description = "The path to the LDIF file to process.";
    ldifFile = new FileArgument('f', "ldifFile", true, 1, "{path}", description,
                                true, true, true, false);
    ldifFile.addLongIdentifier("ldif-file", true);
    parser.addArgument(ldifFile);

    description = "Indicates that the specified LDIF file is compressed " +
                  "using gzip compression.";
    isCompressed = new BooleanArgument('c', "isCompressed", description);
    isCompressed.addLongIdentifier("is-compressed", true);
    parser.addArgument(isCompressed);

    description = "The path to the file to which rejected entries should be " +
                  "written.";
    rejectFile = new FileArgument('R', "rejectFile", false, 1, "{path}",
                                  description, false, true, true, false);
    rejectFile.addLongIdentifier("reject-file", true);
    parser.addArgument(rejectFile);

    description = "The path to a directory containing one or more LDIF files " +
                  "with the schema information to use.  If this is provided, " +
                  "then no LDAP communication will be performed.";
    schemaDirectory = new FileArgument(null, "schemaDirectory", false, 1,
         "{path}", description, true, true, false, true);
    schemaDirectory.addLongIdentifier("schema-directory", true);
    parser.addArgument(schemaDirectory);

    description = "The number of threads to use when processing the LDIF file.";
    numThreads = new IntegerArgument('t', "numThreads", true, 1, "{num}",
         description, 1, Integer.MAX_VALUE, 1);
    numThreads.addLongIdentifier("num-threads", true);
    parser.addArgument(numThreads);

    description = "Ignore validation failures due to entries containing " +
                  "duplicate values for the same attribute.";
    ignoreDuplicateValues =
         new BooleanArgument(null, "ignoreDuplicateValues", description);
    ignoreDuplicateValues.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreDuplicateValues.addLongIdentifier("ignore-duplicate-values", true);
    parser.addArgument(ignoreDuplicateValues);

    description = "Ignore validation failures due to object classes not " +
                  "defined in the schema.";
    ignoreUndefinedObjectClasses =
         new BooleanArgument(null, "ignoreUndefinedObjectClasses", description);
    ignoreUndefinedObjectClasses.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreUndefinedObjectClasses.addLongIdentifier(
         "ignore-undefined-object-classes", true);
    parser.addArgument(ignoreUndefinedObjectClasses);

    description = "Ignore validation failures due to attributes not defined " +
                  "in the schema.";
    ignoreUndefinedAttributes =
         new BooleanArgument(null, "ignoreUndefinedAttributes", description);
    ignoreUndefinedAttributes.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreUndefinedAttributes.addLongIdentifier("ignore-undefined-attributes",
         true);
    parser.addArgument(ignoreUndefinedAttributes);

    description = "Ignore validation failures due to entries with malformed " +
                  "DNs.";
    ignoreMalformedDNs =
         new BooleanArgument(null, "ignoreMalformedDNs", description);
    ignoreMalformedDNs.setArgumentGroupName("Validation Strictness Arguments");
    ignoreMalformedDNs.addLongIdentifier("ignore-malformed-dns", true);
    parser.addArgument(ignoreMalformedDNs);

    description = "Ignore validation failures due to entries with RDN " +
                  "attribute values that are missing from the set of entry " +
                  "attributes.";
    ignoreMissingRDNValues =
         new BooleanArgument(null, "ignoreMissingRDNValues", description);
    ignoreMissingRDNValues.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreMissingRDNValues.addLongIdentifier("ignore-missing-rdn-values", true);
    parser.addArgument(ignoreMissingRDNValues);

    description = "Ignore validation failures due to entries without exactly " +
                  "structural object class.";
    ignoreStructuralObjectClasses =
         new BooleanArgument(null, "ignoreStructuralObjectClasses",
                             description);
    ignoreStructuralObjectClasses.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreStructuralObjectClasses.addLongIdentifier(
         "ignore-structural-object-classes", true);
    parser.addArgument(ignoreStructuralObjectClasses);

    description = "Ignore validation failures due to entries with object " +
                  "classes that are not allowed.";
    ignoreProhibitedObjectClasses =
         new BooleanArgument(null, "ignoreProhibitedObjectClasses",
                             description);
    ignoreProhibitedObjectClasses.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreProhibitedObjectClasses.addLongIdentifier(
         "ignore-prohibited-object-classes", true);
    parser.addArgument(ignoreProhibitedObjectClasses);

    description = "Ignore validation failures due to entries that are " +
                  "one or more superior object classes.";
    ignoreMissingSuperiorObjectClasses =
         new BooleanArgument(null, "ignoreMissingSuperiorObjectClasses",
              description);
    ignoreMissingSuperiorObjectClasses.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreMissingSuperiorObjectClasses.addLongIdentifier(
         "ignore-missing-superior-object-classes", true);
    parser.addArgument(ignoreMissingSuperiorObjectClasses);

    description = "Ignore validation failures due to entries with attributes " +
                  "that are not allowed.";
    ignoreProhibitedAttributes =
         new BooleanArgument(null, "ignoreProhibitedAttributes", description);
    ignoreProhibitedAttributes.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreProhibitedAttributes.addLongIdentifier(
         "ignore-prohibited-attributes", true);
    parser.addArgument(ignoreProhibitedAttributes);

    description = "Ignore validation failures due to entries missing " +
                  "required attributes.";
    ignoreMissingAttributes =
         new BooleanArgument(null, "ignoreMissingAttributes", description);
    ignoreMissingAttributes.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreMissingAttributes.addLongIdentifier("ignore-missing-attributes",
         true);
    parser.addArgument(ignoreMissingAttributes);

    description = "Ignore validation failures due to entries with multiple " +
                  "values for single-valued attributes.";
    ignoreSingleValuedAttributes =
         new BooleanArgument(null, "ignoreSingleValuedAttributes", description);
    ignoreSingleValuedAttributes.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreSingleValuedAttributes.addLongIdentifier(
         "ignore-single-valued-attributes", true);
    parser.addArgument(ignoreSingleValuedAttributes);

    description = "Ignore validation failures due to entries with attribute " +
                  "values that violate their associated syntax.  If this is " +
                  "provided, then no attribute syntax violations will be " +
                  "flagged.  If this is not provided, then all attribute " +
                  "syntax violations will be flagged except for violations " +
                  "in those attributes excluded by the " +
                  "--ignoreSyntaxViolationsForAttribute argument.";
    ignoreAttributeSyntax =
         new BooleanArgument(null, "ignoreAttributeSyntax", description);
    ignoreAttributeSyntax.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreAttributeSyntax.addLongIdentifier("ignore-attribute-syntax", true);
    parser.addArgument(ignoreAttributeSyntax);

    description = "The name or OID of an attribute for which to ignore " +
                  "validation failures due to violations of the associated " +
                  "attribute syntax.  This argument can only be used if the " +
                  "--ignoreAttributeSyntax argument is not provided.";
    ignoreSyntaxViolationsForAttribute = new StringArgument(null,
         "ignoreSyntaxViolationsForAttribute", false, 0, "{attr}", description);
    ignoreSyntaxViolationsForAttribute.setArgumentGroupName(
         "Validation Strictness Arguments");
    ignoreSyntaxViolationsForAttribute.addLongIdentifier(
         "ignore-syntax-violations-for-attribute", true);
    parser.addArgument(ignoreSyntaxViolationsForAttribute);

    description = "Ignore validation failures due to entries with RDNs " +
                  "that violate the associated name form definition.";
    ignoreNameForms = new BooleanArgument(null, "ignoreNameForms", description);
    ignoreNameForms.setArgumentGroupName("Validation Strictness Arguments");
    ignoreNameForms.addLongIdentifier("ignore-name-forms", true);
    parser.addArgument(ignoreNameForms);


    // The ignoreAttributeSyntax and ignoreAttributeSyntaxForAttribute arguments
    // cannot be used together.
    parser.addExclusiveArgumentSet(ignoreAttributeSyntax,
         ignoreSyntaxViolationsForAttribute);
  }



  /**
   * Performs the actual processing for this tool.  In this case, it gets a
   * connection to the directory server and uses it to retrieve the server
   * schema.  It then reads the LDIF file and validates each entry accordingly.
   *
   * @return  The result code for the processing that was performed.
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Get the connection to the directory server and use it to read the schema.
    final Schema schema;
    if (schemaDirectory.isPresent())
    {
      final File schemaDir = schemaDirectory.getValue();

      try
      {
        final TreeMap<String,File> fileMap = new TreeMap<String,File>();
        for (final File f : schemaDir.listFiles())
        {
          final String name = f.getName();
          if (f.isFile() && name.endsWith(".ldif"))
          {
            fileMap.put(name, f);
          }
        }

        if (fileMap.isEmpty())
        {
          err("No LDIF files found in directory " +
              schemaDir.getAbsolutePath());
          return ResultCode.PARAM_ERROR;
        }

        final ArrayList<File> fileList = new ArrayList<File>(fileMap.values());
        schema = Schema.getSchema(fileList);
      }
      catch (final Exception e)
      {
        err("Unable to read schema from files in directory " +
            schemaDir.getAbsolutePath() + ":  " + getExceptionMessage(e));
        return ResultCode.LOCAL_ERROR;
      }
    }
    else
    {
      try
      {
        final LDAPConnection connection = getConnection();
        schema = connection.getSchema();
        connection.close();
      }
      catch (final LDAPException le)
      {
        err("Unable to connect to the directory server and read the schema:  ",
            le.getMessage());
        return le.getResultCode();
      }
    }


    // Create the entry validator and initialize its configuration.
    entryValidator = new EntryValidator(schema);
    entryValidator.setCheckAttributeSyntax(!ignoreAttributeSyntax.isPresent());
    entryValidator.setCheckMalformedDNs(!ignoreMalformedDNs.isPresent());
    entryValidator.setCheckEntryMissingRDNValues(
         !ignoreMissingRDNValues.isPresent());
    entryValidator.setCheckMissingAttributes(
         !ignoreMissingAttributes.isPresent());
    entryValidator.setCheckNameForms(!ignoreNameForms.isPresent());
    entryValidator.setCheckProhibitedAttributes(
         !ignoreProhibitedAttributes.isPresent());
    entryValidator.setCheckProhibitedObjectClasses(
         !ignoreProhibitedObjectClasses.isPresent());
    entryValidator.setCheckMissingSuperiorObjectClasses(
         !ignoreMissingSuperiorObjectClasses.isPresent());
    entryValidator.setCheckSingleValuedAttributes(
         !ignoreSingleValuedAttributes.isPresent());
    entryValidator.setCheckStructuralObjectClasses(
         !ignoreStructuralObjectClasses.isPresent());
    entryValidator.setCheckUndefinedAttributes(
         !ignoreUndefinedAttributes.isPresent());
    entryValidator.setCheckUndefinedObjectClasses(
         !ignoreUndefinedObjectClasses.isPresent());

    if (ignoreSyntaxViolationsForAttribute.isPresent())
    {
      entryValidator.setIgnoreSyntaxViolationAttributeTypes(
           ignoreSyntaxViolationsForAttribute.getValues());
    }


    // Create an LDIF reader that can be used to read through the LDIF file.
    final LDIFReader ldifReader;
    rejectWriter = null;
    try
    {
      InputStream inputStream = new FileInputStream(ldifFile.getValue());
      if (isCompressed.isPresent())
      {
        inputStream = new GZIPInputStream(inputStream);
      }
      ldifReader = new LDIFReader(inputStream, numThreads.getValue(), this);
    }
    catch (final Exception e)
    {
      err("Unable to open the LDIF reader:  ", getExceptionMessage(e));
      return ResultCode.LOCAL_ERROR;
    }

    ldifReader.setSchema(schema);
    if (ignoreDuplicateValues.isPresent())
    {
      ldifReader.setDuplicateValueBehavior(DuplicateValueBehavior.STRIP);
    }
    else
    {
      ldifReader.setDuplicateValueBehavior(DuplicateValueBehavior.REJECT);
    }

    try
    {
      // Create an LDIF writer that can be used to write information about
      // rejected entries.
      try
      {
        if (rejectFile.isPresent())
        {
          rejectWriter = new LDIFWriter(rejectFile.getValue());
        }
      }
      catch (final Exception e)
      {
        err("Unable to create the reject writer:  ", getExceptionMessage(e));
        return ResultCode.LOCAL_ERROR;
      }

      ResultCode resultCode = ResultCode.SUCCESS;
      while (true)
      {
        try
        {
          final Entry e = ldifReader.readEntry();
          if (e == null)
          {
            // Because we're performing parallel processing and returning null
            // from the translate method, LDIFReader.readEntry() should never
            // return a non-null value.  However, it can throw an LDIFException
            // if it encounters an invalid entry, or an IOException if there's
            // a problem reading from the file, so we should still iterate
            // through all of the entries to catch and report on those problems.
            break;
          }
        }
        catch (final LDIFException le)
        {
          malformedEntries.incrementAndGet();

          if (resultCode == ResultCode.SUCCESS)
          {
            resultCode = ResultCode.DECODING_ERROR;
          }

          if (rejectWriter != null)
          {
            try
            {
              rejectWriter.writeComment(
                   "Unable to parse an entry read from LDIF:", false, false);
              if (le.mayContinueReading())
              {
                rejectWriter.writeComment(getExceptionMessage(le), false, true);
              }
              else
              {
                rejectWriter.writeComment(getExceptionMessage(le), false,
                                          false);
                rejectWriter.writeComment("Unable to continue LDIF processing.",
                                          false, true);
                err("Aborting LDIF processing:  ", getExceptionMessage(le));
                return ResultCode.LOCAL_ERROR;
              }
            }
            catch (final IOException ioe)
            {
              err("Unable to write to the reject file:",
                  getExceptionMessage(ioe));
              err("LDIF parse failure that triggered the rejection:  ",
                  getExceptionMessage(le));
              return ResultCode.LOCAL_ERROR;
            }
          }
        }
        catch (final IOException ioe)
        {

          if (rejectWriter != null)
          {
            try
            {
              rejectWriter.writeComment("I/O error reading from LDIF:", false,
                                        false);
              rejectWriter.writeComment(getExceptionMessage(ioe), false,
                                        true);
              return ResultCode.LOCAL_ERROR;
            }
            catch (final Exception ex)
            {
              err("I/O error reading from LDIF:", getExceptionMessage(ioe));
              return ResultCode.LOCAL_ERROR;
            }
          }
        }
      }

      if (malformedEntries.get() > 0)
      {
        out(malformedEntries.get() + " entries were malformed and could not " +
            "be read from the LDIF file.");
      }

      if (entryValidator.getInvalidEntries() > 0)
      {
        if (resultCode == ResultCode.SUCCESS)
        {
          resultCode = ResultCode.OBJECT_CLASS_VIOLATION;
        }

        for (final String s : entryValidator.getInvalidEntrySummary(true))
        {
          out(s);
        }
      }
      else
      {
        if (malformedEntries.get() == 0)
        {
          out("No errors were encountered.");
        }
      }

      return resultCode;
    }
    finally
    {
      try
      {
        ldifReader.close();
      }
      catch (final Exception e) {}

      try
      {
        if (rejectWriter != null)
        {
          rejectWriter.close();
        }
      }
      catch (final Exception e) {}
    }
  }



  /**
   * Examines the provided entry to determine whether it conforms to the
   * server schema.
   *
   * @param  entry           The entry to be examined.
   * @param  firstLineNumber The line number of the LDIF source on which the
   *                         provided entry begins.
   *
   * @return  The updated entry.  This method will always return {@code null}
   *          because all of the real processing needed for the entry is
   *          performed in this method and the entry isn't needed any more
   *          after this method is done.
   */
  public Entry translate(final Entry entry, final long firstLineNumber)
  {
    final ArrayList<String> invalidReasons = new ArrayList<String>(5);
    if (! entryValidator.entryIsValid(entry, invalidReasons))
    {
      if (rejectWriter != null)
      {
        synchronized (this)
        {
          try
          {
            rejectWriter.writeEntry(entry, listToString(invalidReasons));
          }
          catch (final IOException ioe) {}
        }
      }
    }

    final long numEntries = entriesProcessed.incrementAndGet();
    if ((numEntries % 1000L) == 0L)
    {
      out("Processed ", numEntries, " entries.");
    }

    return null;
  }



  /**
   * Converts the provided list of strings into a single string.  It will
   * contain line breaks after all but the last element.
   *
   * @param  l  The list of strings to convert to a single string.
   *
   * @return  The string from the provided list, or {@code null} if the provided
   *          list is empty or {@code null}.
   */
  private static String listToString(final List<String> l)
  {
    if ((l == null) || (l.isEmpty()))
    {
      return null;
    }

    final StringBuilder buffer = new StringBuilder();
    final Iterator<String> iterator = l.iterator();
    while (iterator.hasNext())
    {
      buffer.append(iterator.next());
      if (iterator.hasNext())
      {
        buffer.append(EOL);
      }
    }

    return buffer.toString();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> examples =
         new LinkedHashMap<String[],String>(2);

    String[] args =
    {
      "--hostname", "server.example.com",
      "--port", "389",
      "--ldifFile", "data.ldif",
      "--rejectFile", "rejects.ldif",
      "--numThreads", "4"
    };
    String description =
         "Validate the contents of the 'data.ldif' file using the schema " +
         "defined in the specified directory server using four concurrent " +
         "threads.  All types of validation will be performed, and " +
         "information about any errors will be written to the 'rejects.ldif' " +
         "file.";
    examples.put(args, description);


    args = new String[]
    {
      "--schemaDirectory", "/ds/config/schema",
      "--ldifFile", "data.ldif",
      "--rejectFile", "rejects.ldif",
      "--ignoreStructuralObjectClasses",
      "--ignoreAttributeSyntax"
    };
    description =
         "Validate the contents of the 'data.ldif' file using the schema " +
         "defined in LDIF files contained in the /ds/config/schema directory " +
         "using a single thread.  Any errors resulting from entries that do " +
         "not have exactly one structural object class or from values which " +
         "violate the syntax for their associated attribute types will be " +
         "ignored.  Information about any other failures will be written to " +
         "the 'rejects.ldif' file.";
    examples.put(args, description);

    return examples;
  }



  /**
   * @return EntryValidator
   *
   * Returns the EntryValidator
   */
  public EntryValidator getEntryValidator()
  {
    return entryValidator;
  }
}
