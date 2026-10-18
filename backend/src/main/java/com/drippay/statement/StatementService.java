package com.drippay.statement;

import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.service.TransferService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatementService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final TransferService transferService;

    public StatementService(TransferService transferService) {
        this.transferService = transferService;
    }

    public byte[] exportCsv(Long userId, Instant from, Instant to) {
        List<TransactionResponse> rows = fetchAll(userId, from, to);
        StringBuilder sb = new StringBuilder("id,reference,status,type,amount,category,createdAt\n");
        for (TransactionResponse t : rows) {
            sb.append(t.id()).append(',')
                    .append(escape(t.referenceId())).append(',')
                    .append(t.status()).append(',')
                    .append(t.type()).append(',')
                    .append(t.amount()).append(',')
                    .append(escape(t.categoryCode())).append(',')
                    .append(t.createdAt() != null ? ISO.format(t.createdAt()) : "")
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(Long userId, Instant from, Instant to) {
        List<TransactionResponse> rows = fetchAll(userId, from, to);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("DripPay account statement"));
            doc.add(new Paragraph("Generated: " + ISO.format(Instant.now())));
            doc.add(new Paragraph(" "));
            for (TransactionResponse t : rows) {
                doc.add(new Paragraph(String.format(
                        "%s | %s | %s | ₹%s | %s",
                        t.referenceId(),
                        t.status(),
                        t.type(),
                        t.amount(),
                        t.createdAt() != null ? t.createdAt().atZone(ZoneOffset.UTC).toLocalDate() : "")));
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("PDF export failed", e);
        }
    }

    private List<TransactionResponse> fetchAll(Long userId, Instant from, Instant to) {
        return transferService.listTransactions(userId, null, from, to, PageRequest.of(0, 500))
                .getContent();
    }

    private static String escape(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
