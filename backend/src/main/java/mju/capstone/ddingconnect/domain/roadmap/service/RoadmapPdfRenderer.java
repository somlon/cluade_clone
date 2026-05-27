package mju.capstone.ddingconnect.domain.roadmap.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 데이터 파트 AI 가 생성한 로드맵 JSON 을 PDF 로 렌더링한다.
 *
 * - 결과 화면(피그마)의 카드 레이아웃을 한 장의 PDF 로 옮긴다: 표지 제목 → 3단계 step 카드 → 추천 자격증·활동 → 마지막에 summary_advice.
 * - 한글 폰트는 {@code classpath:fonts/NotoSansKR-Regular.ttf} (SIL OFL 1.1) 를 임베딩한다. 미설정 시 한글이 깨진다.
 * - 호출 시점: 로드맵 생성 직후 1회만. 다운로드 요청마다 재렌더링하지 않는다.
 */
@Component
public class RoadmapPdfRenderer {

    private static final String FONT_RESOURCE = "fonts/NotoSansKR-Regular.ttf";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private BaseFont baseFont;

    @PostConstruct
    void loadFont() {
        try (InputStream in = new ClassPathResource(FONT_RESOURCE).getInputStream()) {
            byte[] fontBytes = in.readAllBytes();
            this.baseFont = BaseFont.createFont(
                    "NotoSansKR-Regular.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    BaseFont.NOT_CACHED,
                    fontBytes,
                    null);
        } catch (Exception e) {
            throw new IllegalStateException("로드맵 PDF 한글 폰트 로드 실패: " + FONT_RESOURCE, e);
        }
    }

    /**
     * 로드맵 content(JSON) 를 PDF 바이트로 변환한다.
     *
     * @param content 데이터 파트 AI 가 생성한 로드맵 JSON 문자열
     * @return PDF 바이트
     * @throws RoadmapHandler JSON 파싱 또는 PDF 렌더 실패 시 {@code ROADMAP_INVALID_CONTENT}
     */
    public byte[] render(String content) {
        RoadmapDoc doc = parse(content);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 56, 48);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            writeTitle(document, doc.roadmapTitle());
            writeSteps(document, doc.steps());
            writeChipSection(document, "추천 자격증", doc.recommendedCertifications());
            writeChipSection(document, "추천 대외활동", doc.recommendedActivities());
            writeSummary(document, doc.summaryAdvice());

        } catch (Exception e) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private RoadmapDoc parse(String content) {
        try {
            return objectMapper.readValue(content, RoadmapDoc.class);
        } catch (Exception e) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
        }
    }

    private void writeTitle(Document document, String title) {
        Font titleFont = new Font(baseFont, 22, Font.BOLD, new Color(33, 33, 33));
        Paragraph p = new Paragraph(title == null ? "" : title, titleFont);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(20f);
        document.add(p);
    }

    private void writeSteps(Document document, List<RoadmapStep> steps) {
        if (steps == null) return;
        Font badgeFont = new Font(baseFont, 10, Font.BOLD, new Color(255, 255, 255));
        Font stepTitleFont = new Font(baseFont, 14, Font.BOLD, new Color(33, 33, 33));
        Font categoryFont = new Font(baseFont, 11, Font.BOLD, new Color(60, 60, 90));
        Font bodyFont = new Font(baseFont, 10.5f, Font.NORMAL, new Color(70, 70, 70));

        for (RoadmapStep step : steps) {
            // phase_badge: 파란 칩 형태 한 행
            PdfPTable badge = new PdfPTable(1);
            badge.setWidthPercentage(100);
            PdfPCell badgeCell = new PdfPCell(new Phrase(nullSafe(step.phaseBadge()), badgeFont));
            badgeCell.setBackgroundColor(new Color(76, 110, 245));
            badgeCell.setBorder(0);
            badgeCell.setPadding(6f);
            badgeCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            badge.addCell(badgeCell);
            document.add(badge);

            // title
            Paragraph t = new Paragraph(step.title(), stepTitleFont);
            t.setSpacingBefore(6f);
            t.setSpacingAfter(8f);
            document.add(t);

            // details: category - content 한 줄씩
            if (step.details() != null) {
                for (StepDetail d : step.details()) {
                    Paragraph row = new Paragraph();
                    row.add(new Chunk(d.category() + " · ", categoryFont));
                    row.add(new Chunk(d.content(), bodyFont));
                    row.setIndentationLeft(8f);
                    row.setSpacingAfter(4f);
                    document.add(row);
                }
            }
            // 구분 여백
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(10f);
            document.add(spacer);
        }
    }

    private void writeChipSection(Document document, String header, List<String> items) {
        if (items == null || items.isEmpty()) return;
        Font headerFont = new Font(baseFont, 13, Font.BOLD, new Color(33, 33, 33));
        Font chipFont = new Font(baseFont, 10.5f, Font.NORMAL, new Color(50, 50, 50));

        Paragraph h = new Paragraph(header, headerFont);
        h.setSpacingBefore(6f);
        h.setSpacingAfter(6f);
        document.add(h);

        PdfPTable chips = new PdfPTable(2);
        chips.setWidthPercentage(100);
        for (String item : items) {
            PdfPCell c = new PdfPCell(new Phrase(nullSafe(item), chipFont));
            c.setBackgroundColor(new Color(240, 242, 247));
            c.setBorder(0);
            c.setPadding(7f);
            chips.addCell(c);
        }
        if (items.size() % 2 == 1) {
            PdfPCell empty = new PdfPCell(new Phrase(" "));
            empty.setBorder(0);
            chips.addCell(empty);
        }
        document.add(chips);

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(8f);
        document.add(spacer);
    }

    private void writeSummary(Document document, String summary) {
        if (summary == null || summary.isBlank()) return;
        document.newPage();
        Font font = new Font(baseFont, 13, Font.NORMAL, new Color(50, 50, 50));
        Paragraph p = new Paragraph(summary, font);
        p.setSpacingBefore(40f);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // ---------- AI JSON 매핑용 내부 record ----------

    private record RoadmapDoc(
            @JsonProperty("roadmap_title") String roadmapTitle,
            @JsonProperty("steps") List<RoadmapStep> steps,
            @JsonProperty("recommended_certifications") List<String> recommendedCertifications,
            @JsonProperty("recommended_activities") List<String> recommendedActivities,
            @JsonProperty("summary_advice") String summaryAdvice
    ) {}

    private record RoadmapStep(
            @JsonProperty("phase_badge") String phaseBadge,
            @JsonProperty("title") String title,
            @JsonProperty("details") List<StepDetail> details
    ) {}

    private record StepDetail(
            @JsonProperty("category") String category,
            @JsonProperty("content") String content
    ) {}
}
