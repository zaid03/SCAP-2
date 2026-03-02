import { Component, HostListener} from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-consulta-bolsas',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, CurrencyPipe],
  templateUrl: './consulta-bolsas.component.html',
  styleUrls: ['./consulta-bolsas.component.css'],
  providers: [CurrencyPipe]
})
export class ConsultaBolsasComponent {
  private fetchCancel$ = new Subject<void>();

  //3 dots menu
  showMenu = false;
  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.showMenu = !this.showMenu;
  }

  @HostListener('document:click')
  closeMenu(): void {
    this.showMenu = false;
  }

  //global variables
  entcod: string | null = null;
  eje: number | null = null;
  cge: string = '';
  creditos: any[] = [];
  private backupCreditos: any[] = [];
  private defaultCreditos: any[] = [];
  wsData: any[] = [];
  public Math = Math;
  page = 0;
  pageSize = 20;
  tableMessage: string = '';
  tableIsError: boolean = false;
  guardarMesage: string = '';
  guardarisError: boolean = false;
  guardarMesageSuccess: string = '';
  guardarisSuccess : boolean = false;

  constructor(private http: HttpClient, private router: Router, private currency: CurrencyPipe) {}

  isLoading: boolean = false;
  ngOnInit(): void {
    this.limpiarMessages();
    this.isLoading = true;
    this.tableIsError = false;

    const ent = sessionStorage.getItem('Entidad');
    const session = sessionStorage.getItem('EJERCICIO');
    const centroGestor = sessionStorage.getItem('CENTROGESTOR');
    if (ent) { const parsed = JSON.parse(ent); this.entcod = parsed.ENTCOD;}
    if (session) { const parsed = JSON.parse(session); this.eje = parsed.eje;}
    if (centroGestor) { const parsed = JSON.parse(centroGestor); this.cge = parsed.value;}

    if (!this.entcod ||  !this.eje || !this.cge) {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }

    this.fetchBolsas();
    this.fetchCentroGestorInfo();
  }

  //main table functions`
  organigrama: string = '';
  programa: string = '';
  description: string = '';
  estado: number = 0;
  fetchCentroGestorInfo() {
    this.http.get<any>(`${environment.backendUrl}/api/cge/search-centros-codigo/${this.entcod}/${this.eje}/${this.cge}`).subscribe({
      next: (res) => {
        this.organigrama = res[0].cgeorg;
        this.programa = res[0].cgefun;
        this.description = res[0].cgedes
        this.estado = res[0].cgecic;
      },
      error: (err) => {
        console.warn(err.error.error ?? err.error);
      }
    })
  }

  fetchBolsas() {
    this.fetchCancel$.next();

    this.http.get<any>(`${environment.backendUrl}/api/gbs/fetch-all/${this.entcod}/${this.eje}/${this.cge}`).subscribe({
      next: (response) => {
        this.creditos = Array.isArray(response) ? [...response] : [];
        this.backupCreditos = [...this.creditos];
        this.defaultCreditos = [...this.creditos];
        this.creditos.forEach((item, idx) => {
          const org = item?.gbsorg ?? '';
          const fun = item?.gbsfun ?? '';
          const eco = item?.gbseco ?? '';
          this.http.get<any>(`${environment.backendUrl}/api/sical/partidas?clorg=${org}&clfun=${fun}&cleco=${eco}`).pipe(takeUntil(this.fetchCancel$)).subscribe({
              next: (partidas) => {
                const partidasArr = Array.isArray(partidas) ? partidas : [];
                this.creditos[idx].partidas = partidasArr;
                const des = partidasArr[0]?.desc ?? '';
                this.creditos[idx].partidaDesc = des;
              },
              error: () => {
                this.creditos[idx].partidas = [];
              },
            });
            this.creditos[idx].saldo = 0;
            this.creditos[idx].limporte = 0;
            this.http.get<any>(`${environment.backendUrl}/api/sical/operaciones?clorg=${org}&clfun=${fun}&cleco=${eco}`).pipe(takeUntil(this.fetchCancel$)).subscribe({
              next: (operaciones) => {
                const operacionesArr = Array.isArray(operaciones) ? operaciones : [];
                this.creditos[idx].operaciones = operacionesArr;
                const firstLinea = operacionesArr[0]?.lineaList?.[0] ?? {};
                const saldo = this.creditos[idx].saldo = firstLinea?.saldo ?? 0;
                const limporte = this.creditos[idx].limporte = firstLinea?.limporte ?? 0;
              },
              error: () => {
                this.creditos[idx].operaciones = [];
              },
            });
        });
        this.sortDirection = 'asc';
        this.page = 0;
        this.updatePagination();
        this.isLoading = false;
      },
      error: (err) => {
        this.tableIsError = true;
        this.tableMessage = err.error.error ?? err.error;
        this.isLoading = false;
      }
    });
  }

  sortField: string | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';
  setSort(field: string) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.applySort();
  }

  applySort() {
    if (!this.sortField) return;
    const field = this.sortField;

    this.creditos = [...this.creditos].sort((a, b) => {
      let aVal: any;
      let bVal: any;

      if (field === 'partidaDesc') {
        aVal = a?.partidas?.[0]?.desc ?? '';
        bVal = b?.partidas?.[0]?.desc ?? '';
        return this.sortDirection === 'asc'
          ? String(aVal).localeCompare(String(bVal), 'es')
          : String(bVal).localeCompare(String(aVal), 'es');
      }

      if (field === 'gbsref') {
        aVal = a?.gbsref ?? '';
        bVal = b?.gbsref ?? '';
        return this.sortDirection === 'asc'
          ? String(aVal).localeCompare(String(bVal), 'es')
          : String(bVal).localeCompare(String(aVal), 'es');
      }

      if (field === 'gbseco' || field === 'gbsope') {
        aVal = Number(a?.[field] ?? 0);
        bVal = Number(b?.[field] ?? 0);
        return this.sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
      }

      if (['limporte', 'saldo', 'gbsiut', 'gbsict'].includes(field)) {
        aVal = this.parseMoney(a?.[field]);
        bVal = this.parseMoney(b?.[field]);
        return this.sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
      }

      if (field === 'acpeco') {
        aVal = this.getkAcPeCo(a.gbsiut, a.gbsict);
        bVal = this.getkAcPeCo(b.gbsiut, b.gbsict);
          return this.sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
        }

      if (field === 'disponible') {
        aVal = this.getkdispon(a.saldo, this.getkAcPeCo(a.gbsiut, a.gbsict));
        bVal = this.getkdispon(b.saldo, this.getkAcPeCo(b.gbsiut, b.gbsict));
        return this.sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
        }

      aVal = a?.[field] ?? '';
      bVal = b?.[field] ?? '';
      return this.sortDirection === 'asc'
        ? String(aVal).localeCompare(String(bVal), 'es')
        : String(bVal).localeCompare(String(aVal), 'es');
    });
    this.page = 0;
    this.updatePagination();
  }

  private updatePagination(): void {
    const total = this.totalPages;
    if (this.page >= total) {
      this.page = Math.max(0, total - 1);
    }
  }

  private startX: number = 0;
  private startWidth: number = 0;
  private resizingColIndex: number | null = null;
  startResize(event: MouseEvent, colIndex: number) {
    this.resizingColIndex = colIndex;
    this.startX = event.pageX;
    const th = (event.target as HTMLElement).parentElement as HTMLElement;
    this.startWidth = th.offsetWidth;

    document.addEventListener('mousemove', this.onResizeMove);
    document.addEventListener('mouseup', this.stopResize);
  }

  onResizeMove = (event: MouseEvent) => {
    if (this.resizingColIndex === null) return;
    const table = document.querySelector('.credito-table') as HTMLTableElement;
    if (!table) return;
    const th = table.querySelectorAll('th')[this.resizingColIndex] as HTMLElement;
    if (!th) return;
    const diff = event.pageX - this.startX;
    th.style.width = (this.startWidth + diff) + 'px';
  };

  stopResize = () => {
    document.removeEventListener('mousemove', this.onResizeMove);
    document.removeEventListener('mouseup', this.stopResize);
    this.resizingColIndex = null;
  };

  DownloadPDF() {
    this.limpiarMessages();

    const source = this.backupCreditos.length ? this.backupCreditos : this.creditos;
    if (!source?.length) {
      this.tableIsError = true;
      this.tableMessage = 'No hay datos para exportar.';
      return;
    }

    const rows = source.map((row: any, index: number) => {
      const aplicacion = `${row.gbsorg ?? ''} - ${row.gbsfun ?? ''} - ${row.gbseco ?? ''}`;
      const acpeco = Number(this.getkAcPeCo(row.gbsiut, row.gbsict) ?? 0);
      const disponible = Number(this.getkdispon(row.saldo, this.getkAcPeCo(row.gbsiut, row.gbsict)) ?? 0);

      return {
        index: index + 1,
        aplicacion,
        desc: row.partidas?.[0]?.desc ?? '',
        gbsope: row.gbsope ?? '',
        gbsref: row.gbsref ?? '',
        limporte: this.formatCurrency(row.limporte),
        saldo: this.formatCurrency(row.saldo),
        acpeco: this.formatCurrency(acpeco),
        disponible: this.formatCurrency(disponible)
      };
    });

    const columns = [
      { header: '#', dataKey: 'index' },
      { header: 'Aplicación', dataKey: 'aplicacion' },
      { header: 'Desc. Aplicación', dataKey: 'desc' },
      { header: 'Operación contable', dataKey: 'gbsope' },
      { header: 'Ref. contable', dataKey: 'gbsref' },
      { header: 'Imp. Operación', dataKey: 'limporte' },
      { header: 'Saldo Operación', dataKey: 'saldo' },
      { header: 'Pte. Contabilizar SCAP', dataKey: 'acpeco' },
      { header: 'Disponible', dataKey: 'disponible' }
    ];

    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(14);
    doc.text('Listado de bolsas', 10, 20);

    autoTable(doc, {
      startY: 30,
      theme: 'plain',
      head: [columns.map(c => c.header)],
      body: rows.map(row => columns.map(c => row[c.dataKey as keyof typeof row] ?? '')),
      styles: { font: 'helvetica', fontSize: 8, cellPadding: 4 },
      headStyles: {
        fillColor: [240, 240, 240],
        textColor: [33, 53, 71],
        fontStyle: 'bold',
        halign: 'left'
      },
      tableLineColor: [200, 200, 200],
      tableLineWidth: 0.5,
      columnStyles: {
        index: { cellWidth: 8 },
        aplicacion: { cellWidth: 32 },
        desc: { cellWidth: 40 },
        gbsope: { cellWidth: 24 },
        gbsref: { cellWidth: 26 },
        limporte: { cellWidth: 24 },
        saldo: { cellWidth: 24 },
        acpeco: { cellWidth: 28 },
        disponible: { cellWidth: 28 }
      }
    });

    doc.save('bolsas.pdf');
  }

  private formatCurrency(value: any): string {
    if (value === null || value === undefined || value === '') return '';
    const numberValue = typeof value === 'number' ? value : Number(value);
    if (isNaN(numberValue)) return '';
    const formatted = new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2
    }).format(numberValue);
    return formatted;
  }

  DownloadCSV() {
    this.limpiarMessages();
    interface Column { header: string; dataKey: string; }
    const columns: Column[] = [
      { header: 'Aplicación', dataKey: 'aplicacion'},
      { header: 'Desc. Aplicación', dataKey: 'desc'},
      { header: 'Operación contable', dataKey: 'gbsope'},
      { header: 'Ref. contable', dataKey: 'gbsref'},
      { header: 'Imp. Operación', dataKey: 'limporte'},
      { header: 'Saldo Operación', dataKey: 'saldo'},
      { header: 'Pte. Contabilizar SCAP', dataKey: 'getkAcPeCo'},
      { header: 'Disponible', dataKey: 'getkdispon'}
    ];

    const rows = (this.creditos || []).map((f: any) => ({
      aplicacion: `${f.gbsorg ?? ''} - ${f.gbsfun ?? ''} - ${f.gbseco ?? ''}`,
      desc: f.partidas[0]?.desc,
      gbsope: f.gbsope,
      gbsref: f.gbsref,
      limporte: this.formatCurrency(f.limporte),
      saldo: this.formatCurrency(f.saldo),
      acpeco: this.formatCurrency(this.getkAcPeCo),
      disponible: this.formatCurrency(this.getkdispon)
    }));

    const escapeCsv = (val: any) => {
      if (val === null || val === undefined) return '';
      let s = String(val);
      s = s.replace(/"/g, '""');
      if (/[,"\r\n]/.test(s)) s = `"${s}"`;
      return s;
    };

    const header = columns.map(c => escapeCsv(c.header)).join(';');
    const bodyLines = rows.map(r => columns.map(c => escapeCsv((r as any)[c.dataKey])).join(';'));

    const csvContent = '\uFEFF' + [header, ...bodyLines].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'Bolsas.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  public getkAcPeCo(gbsiut: any, gbsict: any): string {
    const a = this.parseMoney(gbsiut);
    const b = this.parseMoney(gbsict);
    return Math.round(a - b).toString();
  }

  get paginatedFacturas(): any[] {
    if (!this.creditos || this.creditos.length === 0) return [];
    const start = this.page * this.pageSize;
    return this.creditos.slice(start, start + this.pageSize);
  }
  get totalPages(): number {
    return Math.max(1, Math.ceil((this.creditos?.length ?? 0) / this.pageSize));
  }
  prevPage(): void {
    if (this.page > 0) this.page--;
  }
  nextPage(): void {
    if (this.page < this.totalPages - 1) this.page++;
  }
  goToPage(event: any): void {
    const inputPage = Number(event.target.value);
    if (inputPage >= 1 && inputPage <= this.totalPages) {
      this.page = inputPage - 1;
    }
  }

  cgeSearch: string = '';
  searchBolsas() {
    this.isLoading = true;
    this.fetchCancel$.next();
    this.fetchCentroGestorInfo();
    this.fetchBolsas();
    this.isLoading = false;
  }

  setInputToUpper(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    let upper = (target.value ?? '').toUpperCase();
    if(upper.length > 4) {
      upper = upper.slice(0, 4);
    }
    target.value = upper;
    this.cgeSearch = upper;
  }

  limpiarSearch() {
    this.limpiarMessages();
    this.fetchBolsas();
    this.cgeSearch = '';
  }
  
  //main detail grid functions
  selectedBolsas: any = null;
  showDetails(factura: any) {
    this.limpiarMessages();
    this.guardarisError = false;
    this.guardarisSuccess = false;
    this.selectedBolsas = factura;
    const org = factura?.gbsorg ?? '';
    const fun = factura?.gbsfun ?? '';
    const eco = factura?.gbseco ?? '';
    this.bolsaCombo = `${org} - ${fun} - ${eco}`;
  }

  public bolsaCombo: string = '';
  saveDetails() {
    this.limpiarMessages();
    if (!this.selectedBolsas) return;
    const parts = this.bolsaCombo.split(' - ');
    this.selectedBolsas.gbsorg = parts[0] ?? '';
    this.selectedBolsas.gbsfun = parts[1] ?? '';
    this.selectedBolsas.gbseco = parts[2] ?? '';
  } 

  closeDetails() {
    this.limpiarMessages();
    this.selectedBolsas = null;
  }

  public getkCGECIC(cgecic: any): string {
    const toNum = (v: any) => {
      if (v === null || v === undefined || v === '') return 0;
      const n = Number(v);
      return isNaN(n) ? 0 : n;
    };
    const a = toNum(cgecic);
    if (a === 0) return 'No';
    if ( a === 1) return 'Si';
    if ( a === 2) return 'Cierre para contabilizar';
    return '';
  }

  public getKBoldis(gbsimp: any, gbsibg: any, gbsius: any): string {
    const toNum = (v: any) => {
      if (v === null || v === undefined || v === '') return 0;
      const n = Number(v);
      return isNaN(n) ? 0 : n;
    };
    const a = toNum(gbsimp)
    const b = toNum(gbsibg);
    const c = toNum(gbsius);
    return (a + b -c).toFixed();
  }

  public getkdispon(saldo: any, getkAcPeCo: any): string {
    const a = this.parseMoney(saldo);
    const b = this.parseMoney(getkAcPeCo);
    return Math.round(a - b).toString();
  }

  private parseMoney(val: any): number {
    if (val === null || val === undefined || val === '') return 0;
    if (typeof val === 'number') return val;
    let s = String(val).trim();
    const isParenNeg = /^\(.*\)$/.test(s);
    if (isParenNeg) s = s.replace(/[()]/g, '');
    s = s.replace(/\u00A0/g, ' ').replace(/[^\d.,\-]/g, '');

    if (s.includes(',')) {
      s = s.replace(/\./g, '').replace(',', '.');
    }

    const n = parseFloat(s);
    if (isNaN(n)) return 0;
    return isParenNeg ? -n : n;
  }

  formatGbsimp() {
    if (!this.selectedBolsas || this.selectedBolsas.gbsimp === undefined || this.selectedBolsas.gbsimp === null) return;
    let value = String(this.selectedBolsas.gbsimp)
      .replace(/\s/g, '')
      .replace(/\./g, '')     
      .replace(',', '.')       
      .replace(/[^\d.-]/g, '');
    let num = parseFloat(value);
    if (!isNaN(num)) {
      this.selectedBolsas.gbsimp = num.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
    }
  }
  formateGbsibg() {
    if (!this.selectedBolsas || this.selectedBolsas.gbsibg === undefined || this.selectedBolsas.gbsibg === null) return;
    let value = String(this.selectedBolsas.gbsibg)
      .replace(/\s/g, '')
      .replace(/\./g, '')     
      .replace(',', '.')       
      .replace(/[^\d.-]/g, '');
    let num = parseFloat(value);
    if (!isNaN(num)) {
      this.selectedBolsas.gbsibg = num.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
    }
  }

  cleaningCurrency(value: any) {
    return this.parseMoney(value);
  }

  //misc
  limpiarMessages () {
    this.tableMessage = '';
    this.guardarMesageSuccess = '';
    this.guardarMesage = '';
  }
}
