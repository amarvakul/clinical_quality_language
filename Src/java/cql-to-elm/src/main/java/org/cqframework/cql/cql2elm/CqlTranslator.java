package org.cqframework.cql.cql2elm;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.cql2elm.preprocessor.CqlPreprocessorVisitor;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.fhir.ucum.UcumService;
import org.hl7.cql_annotations.r1.Annotation;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.ObjectFactory;
import org.hl7.elm.r1.Retrieve;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;

import javax.xml.bind.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;

public class CqlTranslator {
    public static enum Options {
        EnableDateRangeOptimization,
        EnableAnnotations,
        EnableLocators,
        EnableResultTypes,
        EnableDetailedErrors,
        DisableListTraversal,
        DisableListDemotion,
        DisableListPromotion,
        EnableIntervalDemotion,
        EnableIntervalPromotion,
        DisableMethodInvocation,
        RequireFromKeyword
    }
    public static enum Format { XML, JSON, COFFEE }
    private static JAXBContext jaxbContext;

    private Library library = null;
    private TranslatedLibrary translatedLibrary = null;
    private Object visitResult = null;
    private List<Retrieve> retrieves = null;
    private List<CqlTranslatorException> exceptions = null;
    private List<CqlTranslatorException> errors = null;
    private List<CqlTranslatorException> warnings = null;
    private List<CqlTranslatorException> messages = null;
    private ModelManager modelManager = null;
    private LibraryManager libraryManager = null;
    private CqlTranslatorException.ErrorSeverity errorLevel = CqlTranslatorException.ErrorSeverity.Info;
    private UcumService ucumService = null;

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, null, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, null, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, null, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, ucumService, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, ucumService, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromText(String cqlText, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) {
        return new CqlTranslator(new ANTLRInputStream(cqlText), modelManager, libraryManager, ucumService, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, null, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager,
                                           CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, null, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager,
                                           CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, null, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, ucumService, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                           CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, ucumService, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromStream(InputStream cqlStream, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                           CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(cqlStream), modelManager, libraryManager, ucumService, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, null, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, null, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, null, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, null, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, null, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, null, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, ucumService, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, ucumService, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(String cqlFileName, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFileName)), modelManager, libraryManager, ucumService, errorLevel, signatureLevel, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, ucumService, CqlTranslatorException.ErrorSeverity.Info, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, ucumService, errorLevel, LibraryBuilder.SignatureLevel.None, options);
    }

    public static CqlTranslator fromFile(File cqlFile, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                                         CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) throws IOException {
        return new CqlTranslator(new ANTLRInputStream(new FileInputStream(cqlFile)), modelManager, libraryManager, ucumService, errorLevel, signatureLevel, options);
    }

    private CqlTranslator(ANTLRInputStream is, ModelManager modelManager, LibraryManager libraryManager, UcumService ucumService,
                          CqlTranslatorException.ErrorSeverity errorLevel, LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) {
        this.modelManager = modelManager;
        this.libraryManager = libraryManager;
        this.ucumService = ucumService;
        translateToELM(is, errorLevel, signatureLevel, options);
    }

    private String toXml(Library library) {
        try {
            return convertToXml(library);
        }
        catch (JAXBException e) {
            throw new IllegalArgumentException("Could not convert library to XML.", e);
        }
    }

    public String toXml() {
        return toXml(library);
    }

    private String toJson(Library library) {
        try {
            return convertToJson(library);
        }
        catch (JAXBException e) {
            throw new IllegalArgumentException("Could not convert library to JSON.", e);
        }
    }

    public String toJson() {
        return toJson(library);
    }

    public Library toELM() {
        return library;
    }

    public TranslatedLibrary getTranslatedLibrary() {
        return translatedLibrary;
    }

    public Object toObject() {
        return visitResult;
    }

    public List<Retrieve> toRetrieves() {
        return retrieves;
    }

    public Map<VersionedIdentifier, TranslatedLibrary> getTranslatedLibraries() {
        return libraryManager.getTranslatedLibraries();
    }

    public Map<String, Library> getLibraries() {
        Map<String, Library> result = new HashMap<String, Library>();
        for (VersionedIdentifier libraryIdentifier: libraryManager.getTranslatedLibraries().keySet()) {
            result.put(libraryIdentifier.getId(), libraryManager.getTranslatedLibraries().get(libraryIdentifier).getLibrary());
        }
        return result;
    }

    public Map<String, String> getLibrariesAsXML() {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<VersionedIdentifier, TranslatedLibrary> entry : libraryManager.getTranslatedLibraries().entrySet()) {
            result.put(entry.getKey().getId(), toXml(entry.getValue().getLibrary()));
        }
        return result;
    }

    public Map<String, String> getLibrariesAsJSON() {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<VersionedIdentifier, TranslatedLibrary> entry : libraryManager.getTranslatedLibraries().entrySet()) {
            result.put(entry.getKey().getId(), toJson(entry.getValue().getLibrary()));
        }
        return result;
    }

    public List<CqlTranslatorException> getExceptions() { return exceptions; }

    public List<CqlTranslatorException> getErrors() { return errors; }

    public List<CqlTranslatorException> getWarnings() { return warnings; }

    public List<CqlTranslatorException> getMessages() { return messages; }

    public static JAXBContext getJaxbContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(Library.class, Annotation.class);
            } catch (JAXBException e) {
                e.printStackTrace();
                throw new RuntimeException("Error creating JAXBContext - " + e.getMessage());
            }
        }
        return jaxbContext;
    }

    private class CqlErrorListener extends BaseErrorListener {

        private LibraryBuilder builder;
        private boolean detailedErrors;

        public CqlErrorListener(LibraryBuilder builder, boolean detailedErrors) {
            this.builder = builder;
            this.detailedErrors = detailedErrors;
        }

        @Override
        public void syntaxError(@NotNull Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, @NotNull String msg, RecognitionException e) {
            TrackBack trackback = new TrackBack(new VersionedIdentifier().withId("unknown"), line, charPositionInLine, line, charPositionInLine);
//            CqlTranslator.this.errors.add(new CqlTranslatorException(msg, trackback, e));

            if (detailedErrors) {
                builder.recordParsingException(new CqlSyntaxException(msg, trackback, e));
                builder.recordParsingException(new CqlTranslatorException(msg, trackback, e));
            }
            else {
                if (offendingSymbol instanceof CommonToken) {
                    CommonToken token = (CommonToken) offendingSymbol;
                    builder.recordParsingException(new CqlSyntaxException(String.format("Syntax error at %s", token.getText()), trackback, e));
                } else {
                    builder.recordParsingException(new CqlSyntaxException("Syntax error", trackback, e));
                }
            }
        }
    }

    private void translateToELM(ANTLRInputStream is, CqlTranslatorException.ErrorSeverity errorLevel,
                                LibraryBuilder.SignatureLevel signatureLevel, CqlTranslator.Options... options) {
        cqlLexer lexer = new cqlLexer(is);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cqlParser parser = new cqlParser(tokens);
        parser.setBuildParseTree(true);

        exceptions = new ArrayList<>();
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
        messages = new ArrayList<>();
        LibraryBuilder builder = new LibraryBuilder(modelManager, libraryManager, ucumService);
        builder.setErrorLevel(errorLevel);
        builder.setSignatureLevel(signatureLevel);
        List<CqlTranslator.Options> optionList = Arrays.asList(options);
        Cql2ElmVisitor visitor = new Cql2ElmVisitor(builder);
        if (optionList.contains(CqlTranslator.Options.EnableDateRangeOptimization)) {
            visitor.enableDateRangeOptimization();
        }
        if (optionList.contains(CqlTranslator.Options.EnableAnnotations)) {
            visitor.enableAnnotations();
        }
        if (optionList.contains(CqlTranslator.Options.EnableLocators)) {
            visitor.enableLocators();
        }
        if (optionList.contains(CqlTranslator.Options.EnableResultTypes)) {
            visitor.enableResultTypes();
        }
        if (optionList.contains(CqlTranslator.Options.EnableDetailedErrors)) {
            visitor.enableDetailedErrors();
        }
        if (optionList.contains(CqlTranslator.Options.DisableListTraversal)) {
            builder.disableListTraversal();
        }
        if (optionList.contains(CqlTranslator.Options.DisableListDemotion)) {
            builder.getConversionMap().disableListDemotion();
        }
        if (optionList.contains(CqlTranslator.Options.DisableListPromotion)) {
            builder.getConversionMap().disableListPromotion();
        }
        if (optionList.contains(CqlTranslator.Options.EnableIntervalDemotion)) {
            builder.getConversionMap().enableIntervalDemotion();
        }
        if (optionList.contains(CqlTranslator.Options.EnableIntervalPromotion)) {
            builder.getConversionMap().enableIntervalPromotion();
        }
        if (optionList.contains(CqlTranslator.Options.DisableMethodInvocation)) {
            visitor.disableMethodInvocation();
        }
        if (optionList.contains(CqlTranslator.Options.RequireFromKeyword)) {
            visitor.enableFromKeywordRequired();
        }

        parser.removeErrorListeners(); // Clear the default console listener
        parser.addErrorListener(new CqlTranslator.CqlErrorListener(builder, visitor.isDetailedErrorsEnabled()));
        ParseTree tree = parser.library();

        CqlPreprocessorVisitor preprocessor = new CqlPreprocessorVisitor();
        preprocessor.visit(tree);

        visitor.setTokenStream(tokens);
        visitor.setLibraryInfo(preprocessor.getLibraryInfo());

        visitResult = visitor.visit(tree);
        library = builder.getLibrary();
        translatedLibrary = builder.getTranslatedLibrary();
        retrieves = visitor.getRetrieves();
        exceptions.addAll(builder.getExceptions());
        errors.addAll(builder.getErrors());
        warnings.addAll(builder.getWarnings());
        messages.addAll(builder.getMessages());
    }

    public String convertToXml(Library library) throws JAXBException {
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(new ObjectFactory().createLibrary(library), writer);
        return writer.getBuffer().toString();
    }

    public String convertToJson(Library library) throws JAXBException {
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty("eclipselink.media-type", "application/json");

        StringWriter writer = new StringWriter();
        marshaller.marshal(new ObjectFactory().createLibrary(library), writer);
        return writer.getBuffer().toString();
    }

    public static void loadModelInfo(File modelInfoXML) {
        final ModelInfo modelInfo = JAXB.unmarshal(modelInfoXML, ModelInfo.class);
        final VersionedIdentifier modelId = new VersionedIdentifier().withId(modelInfo.getName()).withVersion(modelInfo.getVersion());
        final ModelInfoProvider modelProvider = () -> modelInfo;
        ModelInfoLoader.registerModelInfoProvider(modelId, modelProvider);
    }

    private static void outputExceptions(Iterable<CqlTranslatorException> exceptions) {
        for (CqlTranslatorException error : exceptions) {
            TrackBack tb = error.getLocator();
            String lines = tb == null ? "[n/a]" : String.format("[%d:%d, %d:%d]",
                    tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
            System.err.printf("%s:%s %s%n", error.getSeverity(), lines, error.getMessage());
        }
    }

    private static void writeELM(Path inPath, Path outPath, CqlTranslator.Format format, boolean dateRangeOptimizations,
                                 boolean annotations, boolean locators, boolean resultTypes, boolean verifyOnly,
                                 boolean detailedErrors, CqlTranslatorException.ErrorSeverity errorLevel,
                                 boolean disableListTraversal, boolean disableListDemotion, boolean disableListPromotion,
                                 boolean enableIntervalDemotion, boolean enableIntervalPromotion,
                                 boolean disableMethodInvocation, boolean requireFromKeyword, boolean validateUnits,
                                 LibraryBuilder.SignatureLevel signatureLevel) throws IOException {
        ArrayList<CqlTranslator.Options> options = new ArrayList<>();
        if (dateRangeOptimizations) {
            options.add(CqlTranslator.Options.EnableDateRangeOptimization);
        }
        if (annotations) {
            options.add(CqlTranslator.Options.EnableAnnotations);
        }
        if (locators) {
            options.add(CqlTranslator.Options.EnableLocators);
        }
        if (resultTypes) {
            options.add(CqlTranslator.Options.EnableResultTypes);
        }
        if (detailedErrors) {
            options.add(CqlTranslator.Options.EnableDetailedErrors);
        }
        if (disableListTraversal) {
            options.add(CqlTranslator.Options.DisableListTraversal);
        }
        if (disableListDemotion) {
            options.add(CqlTranslator.Options.DisableListDemotion);
        }
        if (disableListPromotion) {
            options.add(CqlTranslator.Options.DisableListPromotion);
        }
        if (enableIntervalDemotion) {
            options.add(CqlTranslator.Options.EnableIntervalDemotion);
        }
        if (enableIntervalPromotion) {
            options.add(CqlTranslator.Options.EnableIntervalPromotion);
        }
        if (disableMethodInvocation) {
            options.add(CqlTranslator.Options.DisableMethodInvocation);
        }
        if (requireFromKeyword) {
            options.add(CqlTranslator.Options.RequireFromKeyword);
        }

        System.err.println("================================================================================");
        System.err.printf("TRANSLATE %s%n", inPath);

        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        UcumService ucumService = null;
        if (validateUnits) {
            try {
                ucumService = new UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
            } catch (UcumException e) {
                System.err.println("Could not create UCUM validation service:");
                e.printStackTrace();
            }
        }
        libraryManager.getLibrarySourceLoader().registerProvider(new DefaultLibrarySourceProvider(inPath.getParent()));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        CqlTranslator translator = fromFile(inPath.toFile(), modelManager, libraryManager, ucumService, errorLevel, signatureLevel, options.toArray(new CqlTranslator.Options[options.size()]));
        libraryManager.getLibrarySourceLoader().clearProviders();

        if (translator.getErrors().size() > 0) {
            System.err.println("Translation failed due to errors:");
            outputExceptions(translator.getExceptions());
        } else if (! verifyOnly) {
            if (translator.getExceptions().size() == 0) {
                System.err.println("Translation completed successfully.");
            }
            else {
                System.err.println("Translation completed with messages:");
                outputExceptions(translator.getExceptions());
            }
            try (PrintWriter pw = new PrintWriter(outPath.toFile(), "UTF-8")) {
                switch (format) {
                    case COFFEE:
                        pw.print("module.exports = ");
                        pw.println(translator.toJson());
                        break;
                    case JSON:
                        pw.println(translator.toJson());
                        break;
                    case XML:
                    default:
                        pw.println(translator.toXml());
                }
                pw.println();
            }
            System.err.println(String.format("ELM output written to: %s", outPath.toString()));
        }

        System.err.println();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        OptionParser parser = new OptionParser();
        OptionSpec<File> input = parser.accepts("input").withRequiredArg().ofType(File.class).required().describedAs("The name of the input file or directory. If a directory is given, all files ending in .cql will be processed");
        OptionSpec<File> model = parser.accepts("model").withRequiredArg().ofType(File.class).describedAs("The name of an input file containing the model info to use for translation. Model info can also be provided through an implementation of ModelInfoProvider");
        OptionSpec<File> output = parser.accepts("output").withRequiredArg().ofType(File.class).describedAs("The name of the output file or directory. If no output is given, an output file name is constructed based on the input name and target format");
        OptionSpec<CqlTranslator.Format> format = parser.accepts("format").withRequiredArg().ofType(CqlTranslator.Format.class).defaultsTo(CqlTranslator.Format.XML).describedAs("The target format for the output");
        OptionSpec verify = parser.accepts("verify");
        OptionSpec optimization = parser.accepts("date-range-optimization");
        OptionSpec annotations = parser.accepts("annotations");
        OptionSpec locators = parser.accepts("locators");
        OptionSpec resultTypes = parser.accepts("result-types");
        OptionSpec detailedErrors = parser.accepts("detailed-errors");
        OptionSpec errorLevel = parser.accepts("error-level").withRequiredArg().ofType(CqlTranslatorException.ErrorSeverity.class).defaultsTo(CqlTranslatorException.ErrorSeverity.Info).describedAs("Indicates the minimum severity message that will be reported. If no error-level is specified, all messages will be output");
        OptionSpec disableListTraversal = parser.accepts("disable-list-traversal");
        OptionSpec disableListDemotion = parser.accepts("disable-list-demotion");
        OptionSpec disableListPromotion = parser.accepts("disable-list-promotion");
        OptionSpec enableIntervalDemotion = parser.accepts("enable-interval-demotion");
        OptionSpec enableIntervalPromotion = parser.accepts("enable-interval-promotion");
        OptionSpec disableMethodInvocation = parser.accepts("disable-method-invocation");
        OptionSpec requireFromKeyword = parser.accepts("require-from-keyword");
        OptionSpec strict = parser.accepts("strict");
        OptionSpec debug = parser.accepts("debug");
        OptionSpec validateUnits = parser.accepts("validate-units");
        OptionSpec<LibraryBuilder.SignatureLevel> signatures = parser.accepts("signatures").withRequiredArg().ofType(LibraryBuilder.SignatureLevel.class).defaultsTo(LibraryBuilder.SignatureLevel.None).describedAs("Indicates whether signatures should be included for invocations in the output ELM. Differing will include invocation signatures that differ from the declared signature. Overloads will include declaration signatures when the operator or function has more than one overload with the same number of arguments as the invocation");

        OptionSet options = parser.parse(args);

        final Path source = input.value(options).toPath();
        final Path destination =
                output.value(options) != null
                        ? output.value(options).toPath()
                        : source.toFile().isDirectory() ? source : source.getParent();
        final CqlTranslator.Format outputFormat = format.value(options);
        final LibraryBuilder.SignatureLevel signatureLevel = signatures.value(options);

        Map<Path, Path> inOutMap = new HashMap<>();
        if (source.toFile().isDirectory()) {
            if (destination.toFile().exists() && ! destination.toFile().isDirectory()) {
                throw new IllegalArgumentException("Output must be a valid folder if input is a folder!");
            }

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toFile().getName().endsWith(".cql") || file.toFile().getName().endsWith(".CQL")) {
                        Path destinationFolder = destination.resolve(source.relativize(file.getParent()));
                        if (! destinationFolder.toFile().exists() && ! destinationFolder.toFile().mkdirs()) {
                            System.err.printf("Problem creating %s%n", destinationFolder);
                        }
                        inOutMap.put(file, destinationFolder);
                    }
                    return CONTINUE;
                }
            });
        } else {
            inOutMap.put(source, destination);
        }

        for (Map.Entry<Path, Path> inOut : inOutMap.entrySet()) {
            Path in = inOut.getKey();
            Path out = inOut.getValue();
            if (out.toFile().isDirectory()) {
                // Use input filename with ".xml", ".json", or ".coffee" extension
                String name = in.toFile().getName();
                if (name.lastIndexOf('.') != -1) {
                    name = name.substring(0, name.lastIndexOf('.'));
                }
                switch (outputFormat) {
                    case JSON:
                        name += ".json";
                        break;
                    case COFFEE:
                        name += ".coffee";
                        break;
                    case XML:
                    default:
                        name += ".xml";
                        break;

                }
                out = out.resolve(name);
            }

            if (out.equals(in)) {
                throw new IllegalArgumentException("input and output file must be different!");
            }

            if (options.has(model)) {
                final File modelFile = options.valueOf(model);
                if (! modelFile.exists() || modelFile.isDirectory()) {
                    throw new IllegalArgumentException("model must be a valid file!");
                }
                loadModelInfo(modelFile);
            }

            writeELM(in, out, outputFormat, options.has(optimization),
                    options.has(debug) || options.has(annotations),
                    options.has(debug) || options.has(locators),
                    options.has(debug) || options.has(resultTypes),
                    options.has(verify),
                    options.has(detailedErrors), // Didn't include in debug, maybe should...
                    options.has(errorLevel)
                            ? (CqlTranslatorException.ErrorSeverity)options.valueOf(errorLevel)
                            : CqlTranslatorException.ErrorSeverity.Info,
                    options.has(strict) || options.has(disableListTraversal),
                    options.has(strict) || options.has(disableListDemotion),
                    options.has(strict) || options.has(disableListPromotion),
                    options.has(enableIntervalDemotion),
                    options.has(enableIntervalPromotion),
                    options.has(strict) || options.has(disableMethodInvocation),
                    options.has(requireFromKeyword),
                    options.has(validateUnits),
                    signatureLevel);
        }
    }
}
