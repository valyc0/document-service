package com.example.ui.view;

import com.example.ui.dto.SearchResultDto;
import com.example.ui.service.OrchestratorClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Route(value = "search", layout = MainLayout.class)
@PageTitle("Search | Document Server")
public class SearchView extends VerticalLayout {

    private final OrchestratorClient orchestratorClient;
    private final TextField searchField;
    private final IntegerField maxResultsField;
    private final VerticalLayout resultsLayout;

    public SearchView(OrchestratorClient orchestratorClient) {
        this.orchestratorClient = orchestratorClient;

        setSpacing(true);
        setPadding(true);

        // Title
        add(new H2("🔍 Search Documents"));

        // Search form
        searchField = new TextField("Search Query");
        searchField.setPlaceholder("Enter search terms...");
        searchField.setWidthFull();

        maxResultsField = new IntegerField("Max Results");
        maxResultsField.setValue(10);
        maxResultsField.setMin(1);
        maxResultsField.setMax(100);
        maxResultsField.setWidth("150px");

        Button searchButton = new Button("🔍 Search", e -> performSearch());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout searchLayout = new HorizontalLayout(searchField, maxResultsField, searchButton);
        searchLayout.setWidthFull();
        searchLayout.setAlignItems(Alignment.END);
        searchField.setWidthFull();
        
        add(searchLayout);

        // Results section
        resultsLayout = new VerticalLayout();
        resultsLayout.setSpacing(true);
        resultsLayout.setPadding(false);
        resultsLayout.setWidthFull();
        add(resultsLayout);

        // Search on Enter
        searchField.addKeyPressListener(event -> {
            if (event.getKey().getKeys().contains("Enter")) {
                performSearch();
            }
        });
    }

    private void performSearch() {
        String query = searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            Notification.show("⚠️ Please enter a search query", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        Integer maxResults = maxResultsField.getValue();
        if (maxResults == null) {
            maxResults = 10;
        }

        try {
            resultsLayout.removeAll();
            resultsLayout.add(new Span("🔄 Searching..."));

            List<SearchResultDto> results = orchestratorClient.searchPost(query.trim(), maxResults);

            resultsLayout.removeAll();

            if (results.isEmpty()) {
                resultsLayout.add(new H3("No results found for: \"" + query + "\""));
                return;
            }

            // Results header
            H3 resultsHeader = new H3(String.format("Found %d result%s for: \"%s\"", 
                    results.size(), 
                    results.size() == 1 ? "" : "s",
                    query));
            resultsLayout.add(resultsHeader);

            // Display results
            for (SearchResultDto result : results) {
                resultsLayout.add(createResultCard(result));
            }

        } catch (Exception e) {
            log.error("Error performing search", e);
            resultsLayout.removeAll();
            resultsLayout.add(new Span("❌ Error: " + e.getMessage()));
            Notification.show("❌ Search error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private VerticalLayout createResultCard(SearchResultDto result) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("background-color", "#fafafa")
                .set("margin-bottom", "10px");

        // Title with score
        HorizontalLayout titleLayout = new HorizontalLayout();
        H4 title = new H4("📄 " + (result.getFilename() != null ? result.getFilename() : "Unknown"));
        title.getStyle().set("margin", "0");
        
        Span scoreSpan = new Span(String.format("Score: %.2f", result.getScore()));
        scoreSpan.getStyle()
                .set("background-color", "#e3f2fd")
                .set("padding", "4px 8px")
                .set("border-radius", "4px")
                .set("font-size", "12px")
                .set("font-weight", "bold");
        
        titleLayout.add(title, scoreSpan);
        titleLayout.setWidthFull();
        titleLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        titleLayout.setAlignItems(Alignment.CENTER);
        
        card.add(titleLayout);

        // Metadata
        if (result.getAuthor() != null || result.getTitle() != null) {
            Span metadata = new Span();
            StringBuilder metaText = new StringBuilder();
            if (result.getTitle() != null) {
                metaText.append("Title: ").append(result.getTitle()).append(" | ");
            }
            if (result.getAuthor() != null) {
                metaText.append("Author: ").append(result.getAuthor()).append(" | ");
            }
            if (result.getPageCount() != null) {
                metaText.append("Pages: ").append(result.getPageCount());
            }
            metadata.setText(metaText.toString());
            metadata.getStyle()
                    .set("font-size", "12px")
                    .set("color", "#666")
                    .set("margin-bottom", "8px");
            card.add(metadata);
        }

        // Highlights
        if (result.getHighlights() != null && !result.getHighlights().isEmpty()) {
            for (String highlight : result.getHighlights()) {
                Div highlightDiv = new Div();
                // Convert <mark> tags to HTML spans with yellow background
                String htmlHighlight = highlight
                        .replace("<mark>", "<span style='background-color: yellow; font-weight: bold;'>")
                        .replace("</mark>", "</span>");
                highlightDiv.getElement().setProperty("innerHTML", "💡 " + htmlHighlight);
                highlightDiv.getStyle()
                        .set("margin-top", "8px")
                        .set("padding", "8px")
                        .set("background-color", "#fff")
                        .set("border-left", "3px solid #2196f3")
                        .set("font-size", "14px");
                card.add(highlightDiv);
            }
        }

        // Document info
        Span docInfo = new Span(String.format("Document ID: %s | Chunk: %d", 
                result.getDocumentId(), 
                result.getChunkIndex() != null ? result.getChunkIndex() : 0));
        docInfo.getStyle()
                .set("font-size", "11px")
                .set("color", "#999")
                .set("margin-top", "8px");
        card.add(docInfo);

        return card;
    }
}
