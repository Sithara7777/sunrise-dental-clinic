package lk.icbt.cis6003.dental.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry in the Help section required by the scenario ("step-by-step
 * instructions for new staff on how to use the system").
 *
 * <p>Help content is served by the API rather than hard-coded into each UI, so
 * the web application and the desktop client always show identical
 * instructions and the text can be corrected without rebuilding the
 * client.</p>
 */
public class HelpTopicDto {

    private String topicId;
    private String title;
    private String summary;
    private String category;
    private int displayOrder;
    private List<String> steps = new ArrayList<>();
    private List<String> tips = new ArrayList<>();

    public HelpTopicDto() {
        // required by Jackson
    }

    public HelpTopicDto(String topicId, String title, String summary, String category, int displayOrder) {
        this.topicId = topicId;
        this.title = title;
        this.summary = summary;
        this.category = category;
        this.displayOrder = displayOrder;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips == null ? new ArrayList<>() : new ArrayList<>(tips);
    }

    public HelpTopicDto addStep(String step) {
        this.steps.add(step);
        return this;
    }

    public HelpTopicDto addTip(String tip) {
        this.tips.add(tip);
        return this;
    }

    @Override
    public String toString() {
        return title;
    }
}
