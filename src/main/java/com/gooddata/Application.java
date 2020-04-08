package com.gooddata;

import com.gooddata.sdk.model.executeafm.afm.PreviousPeriodMeasureDefinition;
import com.gooddata.sdk.model.executeafm.afm.filter.AbsoluteDateFilter;
import com.gooddata.sdk.model.md.Attribute;
import com.gooddata.sdk.model.md.AttributeDisplayForm;
import com.gooddata.sdk.model.md.Entry;
import com.gooddata.sdk.model.md.visualization.VisualizationObject;
import com.gooddata.sdk.model.project.Project;
import com.gooddata.sdk.service.GoodData;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Application {

    private static void processProject(final GoodData goodData, final String projectId, final Writer writer) throws IOException {
        final Project project = goodData.getProjectService().getProjectById(projectId);
        final HashMap<String, Boolean> displayFormCache = new HashMap<>();

        final Collection<Entry> entries = goodData.getMetadataService().find(project, VisualizationObject.class);
        int index = 0;
        for (Entry entry : entries) {
            index++;
            System.out.println(String.format("  (%d/%d) Processing visualization object: %s", index, entries.size(), entry.getUri()));

            final VisualizationObject visualizationObject = goodData.getMetadataService().getObjByUri(entry.getUri(), VisualizationObject.class);

            final boolean hasGlobalDateFilterWithStaticPeriod = visualizationObject.getFilters()
                    .stream()
                    .anyMatch(filter -> filter instanceof AbsoluteDateFilter);

            final boolean hasPreviousPeriodEnabled = visualizationObject.getMeasures()
                    .stream()
                    .anyMatch(measure -> measure.getDefinition() instanceof PreviousPeriodMeasureDefinition);

            final boolean isSlicedByDateNotDay = visualizationObject.getAttributes()
                    .stream()
                    .anyMatch(visualizationAttribute -> {
                        final String displayFormUri = visualizationAttribute.getDisplayForm().getUri();
                        if (displayFormCache.containsKey(displayFormUri)) {
                            return displayFormCache.get(displayFormUri);
                        }

                        final AttributeDisplayForm displayForm = goodData.getMetadataService().getObjByUri(displayFormUri, AttributeDisplayForm.class);
                        final Attribute attribute = goodData.getMetadataService().getObjByUri(displayForm.getFormOf(), Attribute.class);
                        final String type = attribute.getType();
                        final boolean result = type != null && !type.equals("GDC.time.date");
                        displayFormCache.put(displayFormUri, result);

                        return result;
                    });

            if (hasGlobalDateFilterWithStaticPeriod && hasPreviousPeriodEnabled && isSlicedByDateNotDay) {
                System.out.println("    >>> matches search criteria");
                writer.write(visualizationObject.getUri() + "\n");
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        final CommandLine cmd = parseCommandLineArguments(args);
        final GoodData goodData = new GoodData(
                cmd.getOptionValue("hostname", "secure.gooddata.com"),
                cmd.getOptionValue("user"),
                cmd.getOptionValue("password")
        );
        try {
            try (final Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cmd.getOptionValue("output")), UTF_8))) {
                final List<String> lines = Files.readAllLines(Paths.get(cmd.getOptionValue("input")));
                for (int i = 0; i < lines.size(); i++) {
                    final String projectId = lines.get(i);
                    System.out.println(String.format("%d/%d Processing project: %s", i + 1, lines.size(), projectId));
                    processProject(goodData, projectId, writer);
                    System.out.println(String.format("%d%% Done", Math.round((i + 1) / (lines.size() / 100.0))));
                    System.out.println();
                }
            }
        } finally {
            goodData.logout();
        }
    }

    private static CommandLine parseCommandLineArguments(final String[] args) {
        final Options options = new Options();

        final Option hostname = new Option("h", "hostname", true,
                "hostname of the server, for example staging3.intgdc.com or secure.gooddata.com");
        hostname.setRequired(false);
        options.addOption(hostname);

        final Option user = new Option("u", "user", true, "username to login to GoodData server");
        user.setRequired(true);
        options.addOption(user);

        final Option password = new Option("p", "password", true, "password to login to GoodData server");
        password.setRequired(true);
        options.addOption(password);

        final Option input = new Option("i", "input", true, "input file with project IDs, each line with one ID");
        input.setRequired(true);
        options.addOption(input);

        final Option output = new Option("o", "output", true,
                "output file where matching visualization objects will be written");
        output.setRequired(true);
        options.addOption(output);

        CommandLine cmd = null;

        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);

            System.exit(1);
        }
        return cmd;
    }
}
