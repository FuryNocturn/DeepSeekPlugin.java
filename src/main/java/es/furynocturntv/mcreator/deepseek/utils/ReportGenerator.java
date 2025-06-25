package es.furynocturntv.mcreator.deepseek.utils;

import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReportGenerator {
    private final ConversationHistory history;
    private final String recipientEmail;

    public ReportGenerator(ConversationHistory history, String recipientEmail) {
        this.history = history;
        this.recipientEmail = recipientEmail;
    }

    public void sendMonthlyReport() {
        try {
            // Generar contenido del reporte
            String reportContent = generateReportContent();

            // Configurar propiedades de correo
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // Configurar autenticación
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            "furynocturntv@gmail.com",
                            "your_app_specific_password" // Usar contraseña de aplicación
                    );
                }
            });

            // Crear mensaje
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("furynocturntv@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Reporte Mensual DeepSeek Plugin - " + LocalDate.now().getMonth());
            message.setText(reportContent);

            // Enviar correo
            Transport.send(message);

            PluginLogger.log("Monthly report sent to " + recipientEmail);
        } catch (Exception e) {
            PluginLogger.logError("Error sending monthly report", e);
        }
    }

    private String generateReportContent() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        // Filtrar conversaciones del mes actual
        List<ConversationHistory.Conversation> monthlyConversations =
                history.getRecentConversations(Integer.MAX_VALUE).stream()
                        .filter(c -> c.timestamp.toLocalDate().isAfter(monthStart.minusDays(1)))
                        .collect(Collectors.toList());

        // Calcular estadísticas
        int totalConversations = monthlyConversations.size();
        double totalCost = monthlyConversations.stream()
                .mapToDouble(c -> c.cost)
                .sum();

        // Generar reporte
        StringBuilder report = new StringBuilder();
        report.append("Reporte Mensual de DeepSeek Plugin\n");
        report.append("=================================\n\n");
        report.append("Período: ").append(monthStart).append(" a ").append(now).append("\n");
        report.append("Total de interacciones: ").append(totalConversations).append("\n");
        report.append("Costo total: $").append(String.format("%.4f", totalCost)).append("\n\n");
        report.append("Últimas 5 interacciones:\n");

        monthlyConversations.stream()
                .sorted(Comparator.comparing((ConversationHistory.Conversation c) -> c.timestamp).reversed())
                .limit(5)
                .forEach(c -> report.append(formatConversation(c)).append("\n"));

        return report.toString();
    }

    private String formatConversation(ConversationHistory.Conversation conv) {
        return String.format("[%s] %s - Costo: $%.4f",
                conv.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE),
                StringUtils.abbreviate(conv.prompt, 50),
                conv.cost);
    }
}