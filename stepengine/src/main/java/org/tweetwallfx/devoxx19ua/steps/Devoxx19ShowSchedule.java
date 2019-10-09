/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 TweetWallFX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tweetwallfx.devoxx19ua.steps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tweetwallfx.controls.WordleSkin;
import org.tweetwallfx.devoxx.cfp.stepengine.dataprovider.ScheduleDataProvider;
import org.tweetwallfx.devoxx.cfp.stepengine.dataprovider.SessionData;
import org.tweetwallfx.stepengine.api.DataProvider;
import org.tweetwallfx.stepengine.api.Step;
import org.tweetwallfx.stepengine.api.StepEngine.MachineContext;
import org.tweetwallfx.stepengine.api.config.StepEngineSettings;
import org.tweetwallfx.transitions.FlipInXTransition;

/**
 * Devoxx 2019 Show Schedule (Flip In) Animation Step
 *
 * @author Sven Reimers
 */
public class Devoxx19ShowSchedule implements Step {

    private static final Logger LOGGER = LogManager.getLogger(Devoxx19ShowSchedule.class);
    private final Config config;

    private Devoxx19ShowSchedule(Config config) {
        this.config = config;
    }

    @Override
    public void doStep(final MachineContext context) {
        WordleSkin wordleSkin = (WordleSkin) context.get("WordleSkin");
        final ScheduleDataProvider dataProvider = context.getDataProvider(ScheduleDataProvider.class);

        List<FlipInXTransition> transitions = new ArrayList<>();
        if (null == wordleSkin.getNode().lookup("#scheduleNode")) {
            try {
                Node scheduleNode = FXMLLoader.<Node>load(this.getClass().getResource("/schedule.fxml"));
                transitions.add(new FlipInXTransition(scheduleNode));
//                scheduleNode.layoutXProperty().bind(Bindings.multiply(150.0 / 1920.0, wordleSkin.getSkinnable().widthProperty()));
//                scheduleNode.layoutYProperty().bind(Bindings.multiply(200.0 / 1280.0, wordleSkin.getSkinnable().heightProperty()));
                scheduleNode.setLayoutX(config.layoutX);
                scheduleNode.setLayoutY(config.layoutY);
                wordleSkin.getPane().getChildren().add(scheduleNode);

                GridPane grid = (GridPane) scheduleNode.lookup("#sessionGrid");
                int col = 0;
                int row = 0;

                Iterator<SessionData> iterator = dataProvider.getFilteredSessionData().iterator();
                while (iterator.hasNext()) {
                    Node node = createSessionNode(iterator.next());
                    grid.getChildren().add(node);
                    GridPane.setColumnIndex(node, col);
                    GridPane.setRowIndex(node, row);
                    col = (col == 0) ? 1 : 0;
                    if (col == 0) {
                        row++;
                    }
                }
            } catch (IOException ex) {
                LOGGER.error(ex);
            }
        }
        ParallelTransition flipIns = new ParallelTransition();
        flipIns.getChildren().addAll(transitions);
        flipIns.setOnFinished(e -> context.proceed());

        flipIns.play();
    }

    @Override
    public java.time.Duration preferredStepDuration(final MachineContext context) {
        return java.time.Duration.ofMillis(config.stepDuration);
    }

    private Node createSessionNode(final SessionData sessionData) {
        try {
            Node session = FXMLLoader.<Node>load(this.getClass().getResource("/session.fxml"));
            Text title = (Text) session.lookup("#title");
            title.setText(sessionData.title);
            Text speakers = (Text) session.lookup("#speakers");
            speakers.setText(sessionData.speakers.stream().collect(Collectors.joining(", ")));
            Label room = (Label) session.lookup("#room");
            room.setText(sessionData.room);
            Label startTime = (Label) session.lookup("#startTime");
            startTime.setText(sessionData.beginTime + " - " + sessionData.endTime);
            return session;
        } catch (IOException ex) {
            LOGGER.error(ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Implementation of {@link Step.Factory} as Service implementation creating
     * {@link Devoxx19ShowSchedule}.
     */
    public static final class FactoryImpl implements Step.Factory {

        @Override
        public Devoxx19ShowSchedule create(final StepEngineSettings.StepDefinition stepDefinition) {
            return new Devoxx19ShowSchedule(stepDefinition.getConfig(Config.class));
        }

        @Override
        public Class<Devoxx19ShowSchedule> getStepClass() {
            return Devoxx19ShowSchedule.class;
        }

        @Override
        public Collection<Class<? extends DataProvider>> getRequiredDataProviders(final StepEngineSettings.StepDefinition stepSettings) {
            return Arrays.asList(ScheduleDataProvider.class);
        }
    }

    public static class Config extends AbstractConfig {

        public double layoutX = 0;
        public double layoutY = 0;
    }
}
