import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  exportToCSV(data: any[], filename: string, headers: string[]): void {
    if (!data || data.length === 0) {
      return;
    }

    const csvContent = this.convertToCSV(data, headers);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', `${filename}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private convertToCSV(data: any[], headers: string[]): string {
    const separator = ',';
    const csvRows: string[] = [];

    // Add headers
    csvRows.push(headers.join(separator));

    // Add data rows
    for (const item of data) {
      const values = headers.map(header => {
        const value = this.getNestedValue(item, header);
        const escaped = ('' + (value ?? '')).replace(/"/g, '""');
        return `"${escaped}"`;
      });
      csvRows.push(values.join(separator));
    }

    return csvRows.join('\n');
  }

  private getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((current, key) => current?.[key], obj);
  }

  exportToJSON(data: any[], filename: string): void {
    const jsonContent = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', `${filename}.json`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
