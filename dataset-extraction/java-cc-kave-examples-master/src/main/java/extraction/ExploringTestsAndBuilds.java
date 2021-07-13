package extraction;


import cc.kave.commons.model.events.IIDEEvent;
import cc.kave.commons.model.events.testrunevents.TestCaseResult;
import cc.kave.commons.model.events.testrunevents.TestResult;
import cc.kave.commons.model.events.testrunevents.TestRunEvent;
import cc.kave.commons.model.events.visualstudio.BuildEvent;
import cc.kave.commons.model.events.visualstudio.BuildTarget;
import cc.kave.commons.model.events.visualstudio.EditEvent;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.utils.io.ReadingArchive;
import examples.IoHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class ExploringTestsAndBuilds {
    private static final String DIR_USERDATA = "Events-170301-2";
    static String outputPath  = "../../data/";
    static BufferedWriter failedBuildEventsWriter, testEventWriter;

    public ExploringTestsAndBuilds() throws IOException {

    }

    private static void createFileWriters(String userId) throws IOException {
        failedBuildEventsWriter = new BufferedWriter(new FileWriter(outputPath + userId + "_FailedBuild.csv", true));
        //failedBuildEventsWriter.append("date,fail_rate\r\n");
        testEventWriter = new BufferedWriter(new FileWriter(outputPath + userId + "_tests.csv", true));
        //testEventWriter.append("date,duration,success_rate,fail_rate\r\n");
    }

    public static void readAllEvents() throws IOException {
        // each .zip file corresponds to a user
        Set<String> userZips = IoHelper.findAllZips(DIR_USERDATA);
        for (String user : userZips) {
            String userId = (String) user.subSequence(0, 10);
            System.out.println("Processing " + userId);
            createFileWriters(userId);
            File zipFile = Paths.get(DIR_USERDATA, user).toFile();
            ReadingArchive ra = new ReadingArchive(zipFile);
            while (ra.hasNext()) {
                IIDEEvent e = ra.getNext(IIDEEvent.class);
                try {
                    if (e instanceof TestRunEvent) {
                        TestRunEvent testRunEvent = (TestRunEvent) e;
                        if (testRunEvent.WasAborted) continue;

                        int successCount = 0;
                        int failedCount = 0;
                        float total = (float) testRunEvent.Tests.size();
                        //System.out.println(total);
                        String time = testRunEvent.getTriggeredAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
                        for (TestCaseResult x : testRunEvent.Tests) {
                            if (x.Result.equals(TestResult.Success))   successCount++;
                            else if (x.Result.equals(TestResult.Failed))    failedCount++;
                        }
                        if(failedCount == 0) continue;
                        String info = time + ","
                                + testRunEvent.Duration.getSeconds() + ","
                                + successCount/total + ","
                                + failedCount/total + "\r\n";
                        testEventWriter.append(info);
                        //System.out.println(info);

                    }
                    if (e instanceof BuildEvent) {
                        BuildEvent event = (BuildEvent) e;
                        List<BuildTarget> l = event.Targets;
                        float total = (float) l.size();
                        int failCount = 0;
                        for (BuildTarget x : l) {
                            if (!x.Successful)  failCount++;
                        }

                        String time = event.getTriggeredAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
                        String info = time + ","
                                + failCount/total + "\r\n";
                        failedBuildEventsWriter.append(info);
                        failedBuildEventsWriter.flush();
                    }
                } catch (NullPointerException ne) {
                }
            }
            ra.close();
            testEventWriter.flush();
            testEventWriter.close();
            failedBuildEventsWriter.flush();
            failedBuildEventsWriter.close();
        }
    }


    public static void main(String[] args) throws IOException {
        readAllEvents();
    }
}
