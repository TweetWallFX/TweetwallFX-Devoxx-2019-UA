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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tweetwallfx.controls.WordleSkin;
import org.tweetwallfx.devoxx.cfp.stepengine.dataprovider.SpeakerImageProvider;
import org.tweetwallfx.devoxx.cfp.stepengine.dataprovider.TopTalksTodayDataProvider;
import org.tweetwallfx.devoxx.cfp.stepengine.dataprovider.VotedTalk;
import org.tweetwallfx.stepengine.api.DataProvider;
import org.tweetwallfx.stepengine.api.Step;
import org.tweetwallfx.stepengine.api.StepEngine.MachineContext;
import org.tweetwallfx.stepengine.api.config.StepEngineSettings;
import org.tweetwallfx.transitions.FlipInXTransition;

/**
 * Devox 2019 Show Top Rated Talks Today (Flip In) Animation Step
 *
 * @author Sven Reimers
 */
public class Devoxx19ShowTopRatedToday implements Step {

    private static final Logger LOGGER = LogManager.getLogger(Devoxx19ShowTopRatedToday.class);
    private final Config config;

    private Devoxx19ShowTopRatedToday(Config config) {
        this.config = config;
    }

    @Override
    public void doStep(final MachineContext context) {
        WordleSkin wordleSkin = (WordleSkin) context.get("WordleSkin");
        final TopTalksTodayDataProvider dataProvider = context.getDataProvider(TopTalksTodayDataProvider.class);

        List<FlipInXTransition> transitions = new ArrayList<>();
        if (null == wordleSkin.getNode().lookup("#topRatedToday")) {
            try {
                Node scheduleNode = FXMLLoader.<Node>load(this.getClass().getResource("/topratedtalktoday.fxml"));
                transitions.add(new FlipInXTransition(scheduleNode));
                scheduleNode.setLayoutX(config.layoutX);
                scheduleNode.setLayoutY(config.layoutY);
//                scheduleNode.layoutXProperty().bind(Bindings.multiply(150.0 / 1920.0, wordleSkin.getSkinnable().widthProperty()));
//                scheduleNode.layoutYProperty().bind(Bindings.multiply(200.0 / 1280.0, wordleSkin.getSkinnable().heightProperty()));
                wordleSkin.getPane().getChildren().add(scheduleNode);

                GridPane grid = (GridPane) scheduleNode.lookup("#sessionGrid");
                int col = 0;
                int row = 0;

                Iterator<VotedTalk> iterator = dataProvider.getFilteredSessionData().iterator();
                final SpeakerImageProvider speakerImageProvider = context.getDataProvider(SpeakerImageProvider.class);
                while (iterator.hasNext()) {
                    Node node = createTalkNode(iterator.next(), speakerImageProvider);
                    grid.getChildren().add(node);
                    GridPane.setColumnIndex(node, col);
                    GridPane.setRowIndex(node, row);
                    row += 1;
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

    private Node createTalkNode(
            final VotedTalk votingResultTalk,
            final SpeakerImageProvider speakerImageProvider) {
        try {
            Node session = FXMLLoader.<Node>load(this.getClass().getResource("/ratedTalk.fxml"));
            Text title = (Text) session.lookup("#title");
            title.setText(votingResultTalk.proposalTitle);
            Text speakers = (Text) session.lookup("#speakers");
            speakers.setText(votingResultTalk.speakers);
            Label averageVoting = (Label) session.lookup("#averageVote");
            averageVoting.setText(String.format("%.1f", votingResultTalk.ratingAverageScore));
            Label voteCount = (Label) session.lookup("#voteCount");
            voteCount.setText(votingResultTalk.ratingTotalVotes + " Votes");
            ImageView speakerImage = (ImageView) session.lookup("#speakerImage");
            speakerImage.setFitHeight(64);
            speakerImage.setFitWidth(64);
            Rectangle clip = new Rectangle(speakerImage.getFitWidth(), speakerImage.getFitHeight());
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            speakerImage.setClip(clip);
            speakerImage.setImage(speakerImageProvider.getSpeakerImage(votingResultTalk.speaker));
            return session;
        } catch (IOException ex) {
            LOGGER.error(ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean shouldSkip(final MachineContext context) {
        return context.getDataProvider(TopTalksTodayDataProvider.class).getFilteredSessionData().isEmpty();
    }

    @Override
    public Duration preferredStepDuration(MachineContext context) {
        return java.time.Duration.ofMillis(config.stepDuration);
    }

    /**
     * Implementation of {@link Step.Factory} as Service implementation creating
     * {@link Devoxx19ShowTopRatedToday}.
     */
    public static final class FactoryImpl implements Step.Factory {

        @Override
        public Devoxx19ShowTopRatedToday create(final StepEngineSettings.StepDefinition stepDefinition) {
            return new Devoxx19ShowTopRatedToday(stepDefinition.getConfig(Config.class));
        }

        @Override
        public Class<Devoxx19ShowTopRatedToday> getStepClass() {
            return Devoxx19ShowTopRatedToday.class;
        }

        @Override
        public Collection<Class<? extends DataProvider>> getRequiredDataProviders(final StepEngineSettings.StepDefinition stepSettings) {
            return Arrays.asList(
                    TopTalksTodayDataProvider.class,
                    SpeakerImageProvider.class
            );
        }
    }

    public static class Config extends AbstractConfig {

        public double layoutX = 0;
        public double layoutY = 0;
    }
}
