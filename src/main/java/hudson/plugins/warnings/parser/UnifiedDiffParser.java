package hudson.plugins.warnings.parser;

import static hudson.plugins.warnings.parser.Messages.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;
import org.jvnet.localizer.Localizable;

import hudson.model.AbstractBuild;

import hudson.plugins.analysis.util.TreeString;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.analysis.util.model.LineRange;
import hudson.plugins.analysis.util.model.Priority;

/**
 * FIXME: Document type UnifiedDiffParser.
 *
 * @author Paul Krause
 */
public class UnifiedDiffParser extends RegexpLineParser {

    private static final class Diff extends Warning {

        private static final int ORIGINAL = 1;
        private static final int REPLACEMENT = 2;
        private static final int SECTION = 3;

        private static final String WARNING_CATEGORY = "diff";
        private static final String WARNING_TYPE = "diff";
        private static final String WARNING_MESSAGE = Warnings_UnifiedDiffParser_Message();

        private static String message(final Matcher matcher, final String oldfile, final String newfile) {
            final String section = matcher.group(SECTION);
            final String message = String.format(WARNING_MESSAGE, oldfile, newfile);
            return section == null ? message : message + '\n' + section;
        }

        private final TreeString newFile;
        private final Hunk newHunk;

        Diff(final Matcher matcher, final String oldfile, final String newfile) {
            super(oldfile, 0, WARNING_TYPE, WARNING_CATEGORY, message(matcher, oldfile, newfile));
            newFile = TreeString.of(newfile);
            newHunk = Hunk.create(matcher.group(REPLACEMENT));
            addLineRange(Hunk.create(matcher.group(ORIGINAL)));
        }

        /**
         * FIXME: Document method getOtherSide
         * @return
         */
        public FileAnnotation getOtherSide() {
            return new FileAnnotation() {

                @Override
                public int compareTo(final FileAnnotation o) {
                    return Diff.this.compareTo(o);
                }

                @Override
                public void setPathName(final String name) {
                    Diff.this.setPathName(name);
                }

                @Override
                public void setModuleName(final String name) {
                    Diff.this.setModuleName(name);
                }

                @Override
                public void setFileName(final String name) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setContextHashCode(final long code) {
                    Diff.this.setContextHashCode(code);
                }

                @Override
                public boolean isInConsoleLog() {
                    return false;
                }

                @Override
                public boolean hasPackageName() {
                    return Diff.this.hasPackageName();
                }

                @Override
                public String getType() {
                    return Diff.this.getType();
                }

                @Override
                public String getToolTip() {
                    return Diff.this.getToolTip();
                }

                @Override
                public String getTempName(final AbstractBuild<?, ?> owner) {
                    return owner.getRootDir().getAbsolutePath()
                            + "/" + WORKSPACE_FILES
                            + "/" + Integer.toHexString(newFile.hashCode()) + ".tmp";
                }

                @Override
                public String getShortFileName() {
                    return FilenameUtils.getName(TreeString.toString(newFile));
                }

                @Override
                public Priority getPriority() {
                    return Diff.this.getPriority();
                }

                @Override
                public int getPrimaryLineNumber() {
                    return Diff.this.getPrimaryLineNumber();
                }

                @Override
                public String getPathName() {
                    return Diff.this.getPathName();
                }

                @Override
                public String getPackageName() {
                    return Diff.this.getPackageName();
                }

                @Override
                public String getOrigin() {
                    return Diff.this.getOrigin();
                }

                @Override
                public String getModuleName() {
                    return Diff.this.getModuleName();
                }

                @Override
                public String getMessage() {
                    return Diff.this.getMessage();
                }

                @Override
                public String getLinkName() {
                    return Diff.this.getLinkName();
                }

                @Override
                public Collection<LineRange> getLineRanges() {
                    return Arrays.asList(new LineRange(getPrimaryLineNumber()), newHunk);
                }

                @Override
                public long getKey() {
                    return Diff.this.getKey();
                }

                @Override
                public String getFileName() {
                    return TreeString.toString(newFile);
                }

                @Override
                public long getContextHashCode() {
                    return Diff.this.getContextHashCode();
                }

                @Override
                public int getColumnStart() {
                    return Diff.this.getColumnStart();
                }

                @Override
                public int getColumnEnd() {
                    return Diff.this.getColumnEnd();
                }

                @Override
                public String getCategory() {
                    return Diff.this.getCategory();
                }

                @Override
                public boolean canDisplayFile(final AbstractBuild<?, ?> owner) {
                    return Diff.this.canDisplayFile(owner);
                }
            };
        }
    }

    private static final class Hunk extends LineRange {

        /**
         * Creates a new hunk.
         *
         * @param range A hunk range description matching {@link UnifiedDiffParser#RANGE}.
         * @return a new Hunk range matching the description
         */
        static Hunk create(final String range) {
            final int i = range.indexOf(',');
            if (i < 0) {
                return new Hunk(convertLineNumber(range));
            }
            final int start = convertLineNumber(range.substring(0, i));
            final int size = convertLineNumber(range.substring(i)); // slight API abuse
            return new Hunk(start, size);
        }

        private Hunk(final int line) {
            super(line);
        }

        private Hunk(final int start, final int size) {
            super(start, start + size);
        }
    }

    private static final Localizable PARSER_NAME = _Warnings_UnifiedDiffParser_ParserName();
    private static final Localizable LINK_NAME = _Warnings_UnifiedDiffParser_LinkName();
    private static final Localizable TREND_NAME = _Warnings_UnifiedDiffParser_TrendName();
    private static final String RANGE = "\\d+,?\\d*";
    private static final String RANGE_INFO_REGEXP = "^@@ \\-(" + RANGE + ") \\+(" + RANGE + ") @@\\s(.*)$";

    private String oldFileName;
    private String newFileName;

    public UnifiedDiffParser() {
        super(PARSER_NAME, LINK_NAME, TREND_NAME, RANGE_INFO_REGEXP, true);
    }

    /** {@inheritDoc} */
    @Override
    protected Warning createWarning(final Matcher matcher) {
        return new Diff(matcher, oldFileName, newFileName);
    }

    @Override
    protected void findAnnotations(final String content, final List<FileAnnotation> warnings) throws ParsingCanceledException {
        final Matcher matcher = createMatcher(content);

        while (matcher.find()) {
            final Warning warning = createWarning(matcher);
            if (warning != FALSE_POSITIVE) { // NOPMD
                warnings.add(warning);
                if (warning instanceof Diff) {
                    warnings.add(((Diff) warning).getOtherSide());
                }
            }
            if (Thread.interrupted()) {
                throw new ParsingCanceledException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isLineInteresting(final String line) {
        if (line.startsWith("--- ")) {
            final int tab = line.indexOf('\t');
            if (tab > 4) {
                oldFileName = line.substring(4, tab);
            }
            return false;
        }
        if (line.startsWith("+++ ")) {
            final int tab = line.indexOf('\t');
            if (tab > 4) {
                newFileName = line.substring(4, tab);
            }
            return false;
        }
        return line.startsWith("@@");
    }
}