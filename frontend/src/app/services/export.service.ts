import { Injectable } from '@angular/core';
import { Observable, from, of, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

export interface ExportOptions {
  format: 'pdf' | 'json' | 'csv' | 'txt';
  includeMetadata?: boolean;
  dateRange?: { start: Date; end: Date };
  filterByIntent?: string[];
}

export interface ConversationExport {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  messages: Array<{
    sender: string;
    text: string;
    timestamp: string;
    intent?: string;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class ExportService {
// Export conversations to PDF
    exportToPDF(conversations: any[], element?: HTMLElement): Observable<Blob> {
    return from(this.generatePDF(conversations, element)).pipe(
      catchError(error => {
        console.error('[ExportService] Error generating PDF:', error);
        return throwError(() => new Error('Failed to generate PDF'));
      })
    );
  }

  /**
   * Export conversations to JSON
   */
  exportToJSON(conversations: any[]): Observable<string> {
    try {
      const json = JSON.stringify(conversations, null, 2);
      return of(json);
    } catch (error) {
      console.error('[ExportService] Error generating JSON:', error);
      return throwError(() => new Error('Failed to generate JSON'));
    }
  }

  /**
   * Export conversations to CSV
   */
  exportToCSV(conversations: any[]): Observable<string> {
    try {
      const csv = this.convertToCSV(conversations);
      return of(csv);
    } catch (error) {
      console.error('[ExportService] Error generating CSV:', error);
      return throwError(() => new Error('Failed to generate CSV'));
    }
  }

  /**
   * Export conversations to TXT
   */
  exportToTXT(conversations: any[]): Observable<string> {
    try {
      const txt = this.convertToTXT(conversations);
      return of(txt);
    } catch (error) {
      console.error('[ExportService] Error generating TXT:', error);
      return throwError(() => new Error('Failed to generate TXT'));
    }
  }

  /**
   * Download file
   */
  downloadFile(content: string | Blob, filename: string, mimeType?: string): void {
    const blob = content instanceof Blob ? content : new Blob([content], { type: mimeType || 'text/plain' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  /**
   * Generate PDF from conversations or HTML element
   */
  private async generatePDF(conversations: any[], element?: HTMLElement): Promise<Blob> {
    const pdf = new jsPDF('p', 'mm', 'a4');
    const pageWidth = pdf.internal.pageSize.getWidth();
    const margin = 20;
    const contentWidth = pageWidth - 2 * margin;
    let yPosition = margin;

    // Add title
    pdf.setFontSize(20);
    pdf.setFont('helvetica', 'bold');
    pdf.text('Chatbot Conversations Export', margin, yPosition);
    yPosition += 15;

    // Add date
    pdf.setFontSize(10);
    pdf.setFont('helvetica', 'normal');
    pdf.text(`Export Date: ${new Date().toLocaleString()}`, margin, yPosition);
    yPosition += 10;

    // If element is provided, use html2canvas
    if (element) {
      const canvas = await html2canvas(element, {
        scale: 2,
        useCORS: true,
        logging: false
      });

      const imgData = canvas.toDataURL('image/png');
      const imgWidth = contentWidth;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      pdf.addImage(imgData, 'PNG', margin, yPosition, imgWidth, imgHeight);
    } else {
      // Add conversation summaries
      pdf.setFontSize(12);
      pdf.setFont('helvetica', 'bold');
      pdf.text(`Total Conversations: ${conversations.length}`, margin, yPosition);
      yPosition += 10;

      conversations.forEach((conv, index) => {
        if (yPosition > 250) {
          pdf.addPage();
          yPosition = margin;
        }

        pdf.setFontSize(14);
        pdf.setFont('helvetica', 'bold');
        pdf.text(`${index + 1}. ${conv.title || 'Untitled'}`, margin, yPosition);
        yPosition += 7;

        pdf.setFontSize(10);
        pdf.setFont('helvetica', 'normal');
        pdf.text(`Created: ${new Date(conv.createdAt).toLocaleString()}`, margin, yPosition);
        yPosition += 5;
        pdf.text(`Messages: ${conv.messages?.length || 0}`, margin, yPosition);
        yPosition += 10;

        // Add message preview
        if (conv.messages && conv.messages.length > 0) {
          const preview = conv.messages.slice(0, 3);
          preview.forEach((msg: { sender: string; text: string; timestamp: string; intent?: string }) => {
            if (yPosition > 270) {
              pdf.addPage();
              yPosition = margin;
            }
            const sender = msg.sender === 'user' ? 'You' : 'Bot';
            const text = this.truncateText(msg.text, 80);
            pdf.text(`[${sender}] ${text}`, margin + 5, yPosition);
            yPosition += 5;
          });
          if (conv.messages.length > 3) {
            pdf.text(`... and ${conv.messages.length - 3} more messages`, margin + 5, yPosition);
            yPosition += 5;
          }
        }
        yPosition += 5;
      });
    }

    return pdf.output('blob');
  }

  /**
   * Convert conversations to CSV format
   */
  private convertToCSV(conversations: any[]): string {
    const headers = ['Conversation ID', 'Title', 'Created At', 'Message Index', 'Sender', 'Text', 'Intent', 'Timestamp'];
    const rows: string[][] = [headers];

    conversations.forEach(conv => {
      if (conv.messages && Array.isArray(conv.messages)) {
        conv.messages.forEach((msg: any, index: number) => {
          rows.push([
            conv.id || '',
            this.escapeCSV(conv.title || ''),
            new Date(conv.createdAt).toISOString(),
            index.toString(),
            msg.sender || '',
            this.escapeCSV(msg.text || ''),
            msg.intent || '',
            new Date(msg.timestamp).toISOString()
          ]);
        });
      } else {
        rows.push([
          conv.id || '',
          this.escapeCSV(conv.title || ''),
          new Date(conv.createdAt).toISOString(),
          '',
          '',
          '',
          '',
          ''
        ]);
      }
    });

    return rows.map(row => row.join(',')).join('\n');
  }

  /**
   * Convert conversations to TXT format
   */
  private convertToTXT(conversations: any[]): string {
    let txt = 'CHATBOT CONVERSATIONS EXPORT\n';
    txt += '='.repeat(50) + '\n';
    txt += `Export Date: ${new Date().toLocaleString()}\n`;
    txt += `Total Conversations: ${conversations.length}\n\n`;

    conversations.forEach((conv, index) => {
      txt += `${'='.repeat(50)}\n`;
      txt += `Conversation ${index + 1}: ${conv.title || 'Untitled'}\n`;
      txt += `ID: ${conv.id || 'N/A'}\n`;
      txt += `Created: ${new Date(conv.createdAt).toLocaleString()}\n`;
      txt += `Updated: ${new Date(conv.updatedAt).toLocaleString()}\n`;
      txt += `${'='.repeat(50)}\n\n`;

      if (conv.messages && Array.isArray(conv.messages)) {
        conv.messages.forEach((msg: any) => {
          const sender = msg.sender === 'user' ? 'YOU' : 'BOT';
          const timestamp = new Date(msg.timestamp).toLocaleString();
          txt += `[${timestamp}] ${sender}:\n`;
          txt += `${msg.text}\n`;
          if (msg.intent) {
            txt += `(Intent: ${msg.intent})\n`;
          }
          txt += '\n';
        });
      }
      txt += '\n';
    });

    return txt;
  }

  /**
   * Escape CSV special characters
   */
  private escapeCSV(text: string): string {
    if (text.includes(',') || text.includes('"') || text.includes('\n')) {
      return `"${text.replace(/"/g, '""')}"`;
    }
    return text;
  }

  /**
   * Truncate text to specified length
   */
  private truncateText(text: string, maxLength: number): string {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength - 3) + '...';
  }
  //Print conversations
     printConversations(conversations: any[]): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;

    const html = this.generatePrintHTML(conversations);
    printWindow.document.write(html);
    printWindow.document.close();
    printWindow.print();
  }

  /**
   * Generate HTML for printing
   */
  private generatePrintHTML(conversations: any[]): string {
    let html = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Chatbot Conversations</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; }
          .header { border-bottom: 2px solid #333; margin-bottom: 20px; }
          .conversation { margin-bottom: 30px; border: 1px solid #ddd; padding: 15px; }
          .conversation-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; }
          .conversation-meta { color: #666; font-size: 12px; margin-bottom: 15px; }
          .message { margin: 10px 0; padding: 10px; border-radius: 5px; }
          .message.user { background: #e3f2fd; margin-left: 20px; }
          .message.bot { background: #f5f5f5; margin-right: 20px; }
          .message-sender { font-weight: bold; font-size: 12px; }
          .message-text { margin-top: 5px; }
          .message-intent { font-size: 11px; color: #666; font-style: italic; }
        </style>
      </head>
      <body>
        <div class="header">
          <h1>Chatbot Conversations Export</h1>
          <p>Export Date: ${new Date().toLocaleString()}</p>
          <p>Total Conversations: ${conversations.length}</p>
        </div>
    `;

    conversations.forEach((conv, index) => {
      html += `
        <div class="conversation">
          <div class="conversation-title">${index + 1}. ${conv.title || 'Untitled'}</div>
          <div class="conversation-meta">
            Created: ${new Date(conv.createdAt).toLocaleString()} |
            Messages: ${conv.messages?.length || 0}
          </div>
      `;

      if (conv.messages && Array.isArray(conv.messages)) {
        conv.messages.forEach((msg: any) => {
          html += `
            <div class="message ${msg.sender}">
              <div class="message-sender">${msg.sender === 'user' ? 'You' : 'Bot'} - ${new Date(msg.timestamp).toLocaleString()}</div>
              <div class="message-text">${msg.text}</div>
              ${msg.intent ? `<div class="message-intent">Intent: ${msg.intent}</div>` : ''}
            </div>
          `;
        });
      }

      html += '</div>';
    });

    html += '</body></html>';
    return html;
  }
}
