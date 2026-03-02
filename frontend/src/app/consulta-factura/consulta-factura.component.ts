import { Component, HostListener} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, JsonPipe } from '@angular/common';
import { SidebarComponent } from '../sidebar/sidebar.component';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-consulta-factura',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './consulta-factura.component.html',
  styleUrls: ['./consulta-factura.component.css']
})

export class ConsultaFacturaComponent {
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
  private entcod: number | null = null;
  private eje: number | null = null;
  public centroGestor: string = '';
  public estadogc: number | null = null;
  public facturaSearchTouched: boolean = false;
  public searchQueryTouched: boolean = false;
  private initialCentroGestor: string = '';
  facturas: any[] = [];
  private backupFacturas: any[] = [];
  page = 0;
  pageSize = 20;
  facturaMessageIsSuccess: boolean = false;
  estadoMessage: string = '';
  isEstadoMessage: boolean = false;
  public Math = Math;

  constructor(private http: HttpClient, private router: Router) {}

  private defaultFacturas: any[] = [];
  isLoading: boolean = false;
  ngOnInit(): void{
    this.limpiarMEssages();
    this.isLoading = true;
    const entidad = sessionStorage.getItem('Entidad');
    const eje = sessionStorage.getItem('EJERCICIO');
    const cge = sessionStorage.getItem('CENTROGESTOR');
    const estadoCentroGestor = sessionStorage.getItem('ESTADOGC');

    if(estadoCentroGestor){
      const parsed = JSON.parse(estadoCentroGestor);
      this.estadogc = parsed.value;

      if (this.estadogc === 1) {
        this.isEstadoMessage = true;
        this.estadoMessage = 'Centro Gestor CERRADO';
      }
      if ( this.estadogc === 2) {
        this.isEstadoMessage = true;
        this.estadoMessage = 'Centro Gestor CERRADO para CONTABILIZAR';
      }
    }

    if (cge){
      const parsed = JSON.parse(cge);
      this.centroGestor = parsed.value
      this.initialCentroGestor = this.centroGestor;
    }
    
    if (entidad) {
      const parsed = JSON.parse(entidad);
      this.entcod = parsed.ENTCOD;
    }
    if (eje) {
      const parsed = JSON.parse(eje);
      this.eje = parsed.eje;
    }

    if (!entidad || this.entcod === null || !eje || this.eje === null) {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }

    this.fetchFacturas();
  }

  fetchFacturas() {
    this.http.get<any>(`${environment.backendUrl}/api/fac/${this.entcod}/${this.eje}/${this.centroGestor}`).subscribe({
      next: (response) => {
        if (!Array.isArray(response) || response.length === 0) {
          this.facturaMessageIsSuccess = true;
          this.filterFacturaMessage = 'No hay facturas por las medidas de búsqueda';
          this.facturas = [];
          this.defaultFacturas = [];
          this.updatePagination();
          this.isLoading = false;
        } else {
          this.facturas = response;
          this.backupFacturas = Array.isArray(response) ? [...response] : [];
          this.defaultFacturas = [...this.backupFacturas];
          this.page = 0;
          this.updatePagination();
          this.isLoading = false;
        }
      }, error: (err) => {
        this.filterFacturaMessage = err.error.error ?? err.error;
        this.isLoading = false;
      }
    });
  }

  //main table functions
  sortField: 'facnum' | 'tercod' | 'ternom' | 'ternif' | 'facfre' | 'facimp' | 'facdoc' | 'facann' | 'facfac' | 'facdat' | 'facado' | 'facfco' |'getPendingApply(p)' | 'cgecod' | 'getStaus(p.facado, p.facimp, p.faciec, p.facidi)' | null = null;
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  private defaultProveedores: any[] = [];
  toggleSort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applySort();
    this.page = 0;
    this.updatePagination();
  }

  private applySort(): void {
    if (!this.sortColumn) return;
    this.facturas.sort((a, b) => {
      let aValue: any;
      let bValue: any;

      if (this.sortColumn === 'getPendingApply(p)') {
        aValue = this.getPendingApply(a);
        bValue = this.getPendingApply(b);
      } else if (this.sortColumn === 'getStaus(p.facado, p.facimp, p.faciec, p.facidi)') {
        aValue = this.getStaus(a.facado, a.facimp, a.faciec, a.facidi);
        bValue = this.getStaus(b.facado, b.facimp, b.faciec, b.facidi);
      } else {
        aValue = a[this.sortColumn];
        bValue = b[this.sortColumn];
      }

      const aNum = Number(aValue);
      const bNum = Number(bValue);
      if (!isNaN(aNum) && !isNaN(bNum)) {
        return this.sortDirection === 'asc' ? aNum - bNum : bNum - aNum;
      }

      aValue = (aValue ?? '').toString().toUpperCase();
      bValue = (bValue ?? '').toString().toUpperCase();
      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  private updatePagination(): void {
    const total = this.totalPages;
    if (total === 0) {
      this.page = 0;
      return;
    }
    if (this.page >= total) {
      this.page = total - 1;
    }
  }

  get paginatedFacturas(): any[] {
    if (!this.facturas || this.facturas.length === 0) return [];
    const start = this.page * this.pageSize;
    return this.facturas.slice(start, start + this.pageSize);
  }
  get totalPages(): number {
    return Math.max(1, Math.ceil((this.facturas?.length ?? 0) / this.pageSize));
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
    const table = document.querySelector('.facturas-table') as HTMLTableElement;
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

  formatDate = (v: any) => {
    if (!v && v !== 0) return '';
    const s = String(v);
    const d = new Date(s);
    return isNaN(d.getTime()) ? s : d.toLocaleDateString('es-ES');
  };

  formatCurrency = (value: any) => {
    if (value === null || value === undefined || value === '') return '';
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2
    }).format(Number(value));
  };

  DownloadPDF() {
    this.limpiarMEssages();

    const source = this.backupFacturas.length ? this.backupFacturas : this.facturas;
    if (!source?.length) {
      this.filterFacturaMessage = 'No hay datos para exportar.';
      return;
    }

    const rows = source.map((row: any, index: number) => ({
      index: index + 1,
      facnum: row.facnum ?? '',
      tercod: row.tercod ?? '',
      ternom: row.ternom ?? '',
      ternif: row.ternif ?? '',
      facfre: this.formatDate(row.facfre),
      facimp: this.formatCurrency(row.facimp),
      facdoc: row.facdoc ?? '',
      facann: row.facann ?? '',
      facfac: row.facfac ?? '',
      facdat: this.formatDate(row.facdat),
      facado: row.facado ?? '',
      facfco: this.formatDate(row.facfco),
      pendingApply: this.formatCurrency(this.getPendingApply(row)),
      cgecod: row.cgecod ?? '',
      estado: this.getStaus(row.facado, row.facimp, row.faciec, row.facidi)
    }));

    const columns = [
      { header: '#', dataKey: 'index' },
      { header: 'Número Registro', dataKey: 'facnum' },
      { header: 'Código Prov', dataKey: 'tercod' },
      { header: 'Nombre Proveedor', dataKey: 'ternom' },
      { header: 'NIF Prov', dataKey: 'ternif' },
      { header: 'F.Registro', dataKey: 'facfre' },
      { header: 'Importe total', dataKey: 'facimp' },
      { header: 'Núm. Factura', dataKey: 'facdoc' },
      { header: 'Año', dataKey: 'facann' },
      { header: 'R.C.F', dataKey: 'facfac' },
      { header: 'F.Factura', dataKey: 'facdat' },
      { header: 'ADO', dataKey: 'facado' },
      { header: 'F. Contable', dataKey: 'facfco' },
      { header: 'Pte. Aplicar', dataKey: 'pendingApply' },
      { header: 'C. Gestor', dataKey: 'cgecod' },
      { header: 'Estado', dataKey: 'estado' }
    ];

    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(14);
    doc.text('Listado de facturas', 10, 20);

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
        facnum: { cellWidth: 18 },
        tercod: { cellWidth: 18 },
        ternom: { cellWidth: 36 },
        ternif: { cellWidth: 20 },
        facfre: { cellWidth: 20 },
        facimp: { cellWidth: 22 },
        facdoc: { cellWidth: 20 },
        facann: { cellWidth: 15 },
        facfac: { cellWidth: 18 },
        facdat: { cellWidth: 20 },
        facado: { cellWidth: 15 },
        facfco: { cellWidth: 20 },
        pendingApply: { cellWidth: 22 },
        cgecod: { cellWidth: 15 },
        estado: { cellWidth: 24 }
      }
    });

    doc.save('facturas.pdf');
  }

  DownloadCSV() {
    this.limpiarMEssages();
    if(this.facturas.length === 0) {
      this.filterFacturaMessage = 'He has no data to export.';
      return
    }
    
    interface Column { header: string; dataKey: string; }

    const columns: Column[] = [
      { header: 'Número Registro', dataKey: 'facnum'},
      { header: 'Código Prov', dataKey: 'tercod'},
      { header: 'Nombre Proveedor', dataKey: 'ternom'},
      { header: 'NIF Prov', dataKey: 'ternif'},
      { header: 'F.Registro', dataKey: 'facfre'},
      { header: 'Importe total', dataKey: 'facimp'},
      { header: 'Núm. Factura', dataKey: 'facdoc'},
      { header: 'Año', dataKey: 'facann'},
      { header: 'R.C.F', dataKey: 'facfac'},
      { header: 'F.Factura', dataKey: 'facdat'},
      { header: 'ADO', dataKey: 'facado'},
      { header: 'F. contable', dataKey: 'facfco'},
      { header: 'Pte. Aplicar', dataKey: 'pendingApply'},
      { header: 'C. Gestor', dataKey: 'cgecod'},
      { header: 'Estado', dataKey: 'estado'}
    ];

    const rows = (this.facturas || []).map((p: any) => ({
      facnum: p.facnum,
      tercod: p.tercod,
      ternom: p.ternom,
      ternif: p.ternif,
      facfre: this.formatDate(p.facfre),
      facimp: this.formatCurrency(p.facimp),
      facdoc: p.facdoc,
      facann: p.facann,
      facfac: p.facfac,
      facdat: this.formatDate(p.facdat),
      facado: p.facado,
      facfco: this.formatDate(p.facfco),
      pendingApply: this.formatCurrency(this.getPendingApply(p)),
      cgecod: p.cgecod,
      estado: this.getStaus(p.facado, p.facimp, p.faciec, p.facidi)
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
    a.download = 'Facturas.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  //detail grid functions
  selectedFacturas: any = null;
  detallesMessage: String = '';
  fettalesIsError: boolean = false;

  showDetails(factura: any) {
    this.limpiarMEssages();
    this.selectedFacturas = factura;
    this.detailView = 'Albaranes';
    this.setAlbaranesOptio('albaranes', factura?.facnum);
  }

  closeDetails() {
    this.selectedFacturas = null;
    this.limpiarMEssages();
  }

  public getPendingApply(f: any): number {
    if (!f) return 0;
    const toNum = (v: any) => (v === null || v === undefined || v === '' ? 0 : Number(v) || 0);
    let pending = toNum(f.facimp) - (toNum(f.faciec) + toNum(f.facidi));
    return Math.round(pending * 100) / 100;
  }

  public getStaus(facado: any, facimp: any, faciec: any, facidi:any ){
    const toNum = (v: any) => {
      if (v === null || v === undefined || v === '') return 0;
      const n = Number(v);
      return isNaN(n) ? 0 : n;
    };

    const FACADO = toNum(facado);
    const FACIEC = toNum(faciec);
    const FACIMP = toNum(facimp);
    const FACIDI = toNum(facidi);

    if (FACADO != 0){
      return 'contbilizadas';
    }
    if (facado === '' && (this.Math.round((FACIMP * 100) / 100) === this.Math.round((FACIEC + FACIDI) * 100) / 100)) {
      return 'Pte. Aplicada';
    }
    if (facado === '' && (this.Math.round((FACIMP * 100) / 100) != this.Math.round((FACIEC + FACIDI) * 100) / 100)) {
      return 'Pte. Sin aplicar';
    }
    return '';
  }

  //search functions
  fechaTipo: 'registro' | 'factura' | 'contable' | '' = '';
  estadoTipo: 'contabilizadas' | 'no-contabilizadas' | 'aplicadas' | 'sin-aplicadas' | '' = 'no-contabilizadas';
  fromDate: string = '';
  toDate: string = '';
  filterFacturaMessage: string = '';
  facturaSearch: string = '';
  public searchQuery: string = '';

  onEjeInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const sanitized = input.value.replace(/\D+/g, '').slice(0, 4);
    if (sanitized !== this.facturaSearch) {
      this.facturaSearch = sanitized;
    }
    this.facturaSearchTouched = true;
  }
  
  filterFacturas(): void {
    this.limpiarMEssages();
    this.isLoading = true;
    if (this.entcod == null || this.eje == null || !this.centroGestor) {
      this.filterFacturaMessage = 'Faltan datos de sesión.';
      this.isLoading = false;
      return;
    }

    if (!this.searchQuery && !this.fechaTipo && !this.fromDate && !this.toDate && !this.facturaSearch) {
      this.fetchFacturas();
      this.isLoading = false;
      return;
    }

    this.filterFacturaMessage = '';

    const estadoMap: Record<string, string> = {
      'contabilizadas': 'CONT',
      'no-contabilizadas': 'NO_CONT',
      'aplicadas': 'PTE_APL',
      'sin-aplicadas': 'PTE_SIN',
      'todas': 'TODAS',
      '': 'NO_CONT' 
    };
    const fechaMap: Record<string, string> = {
      'registro': 'REGISTRO',
      'factura': 'FACTURA',
      'contable': 'CONTABLE',
      '': 'REGISTRO'
    };

    const estado = estadoMap[this.estadoTipo ?? ''] ?? 'NO_CONT';
    const dateType = fechaMap[this.fechaTipo ?? ''] ?? 'REGISTRO';

    const desde = this.fromDate?.trim() || '';
    const hasta = this.toDate?.trim() || '';
    if ((desde || hasta) && !this.fechaTipo) {
      this.filterFacturaMessage = 'Seleccione un tipo de fecha antes de buscar.';
      this.isLoading = false;
      return;
    }

    const facann = this.facturaSearch?.trim() || '';
    let facannMode = 'ANY';
    
    if (facann) {
      facannMode = 'VALUE';
    } else {
      if (estado === 'CONT') {
        facannMode = 'NOT_NULL';
      } else if (estado === 'NO_CONT' || estado === 'PTE_APL' || estado === 'PTE_SIN') {
        facannMode = 'NULL';
      }
    }

    const searchRaw = this.searchQuery?.trim() || '';
    const digits = searchRaw.replace(/\D/g, '');
    const hasLetters = /[A-Za-z]/.test(searchRaw);
    const hasDigits = /\d/.test(searchRaw);
    const NIF_MIN_DIGITS = 5;
    let searchType = 'OTROS';
    let search = '';

    if (searchRaw) {
      if (!hasLetters && hasDigits && digits.length > 0 && digits.length <= NIF_MIN_DIGITS) {
        searchType = 'TERCOD';
        search = digits;
      } else if (!hasLetters && hasDigits && digits.length > NIF_MIN_DIGITS) {
        searchType = 'NIF';
        search = digits;
      } else if (hasLetters && hasDigits && searchRaw.length > NIF_MIN_DIGITS) {
        searchType = 'NIF_LETTERS';
        search = searchRaw;
      } else {
        searchType = 'OTROS';
        search = searchRaw;
      }
    }

    const params: Record<string, string> = {
      ent: String(this.entcod),
      eje: String(this.eje),
      cgecod: this.centroGestor,
      estado,
      dateType,
      facannMode
    };
    
    if (desde) params['fromDate'] = desde; 
    if (hasta) params['toDate'] = hasta;    
    if (facann && facannMode === 'VALUE') params['facann'] = facann;
    if (search) {
      params['search'] = search;
      params['searchType'] = searchType;
    }

    this.http.get<any[]>(`${environment.backendUrl}/api/fac/search`, { observe: 'response', params })
    .subscribe({
      next: (res) => {
        if (res.status === 204 || !res.body || res.body.length === 0) {
          this.facturaMessageIsSuccess = true;
          this.filterFacturaMessage = 'No resultado';
          this.facturas = [];
          this.defaultFacturas = [];
          this.sortField = null;
          this.sortDirection = 'asc';
          this.updatePagination();
          this.isLoading = false;
          return;
        }
        const body = res.body ?? [];
        this.facturas = body;
        this.defaultFacturas = [...body];
        if (this.sortField) {
          this.applySort();
        } else {
          this.page = 0;
          this.updatePagination();
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.filterFacturaMessage = err.error.error ?? err.error;
        this.isLoading = false;
      }
    });
  }

  forgetAll(): void{
    this.limpiarMEssages();
    this.facturaSearch = '';
    this.searchQuery = '';
    this.centroGestor = this.initialCentroGestor || '';
    this.facturaSearchTouched = false;
    this.searchQueryTouched = false;
    this.fechaTipo = '';
    this.estadoTipo = 'no-contabilizadas';
    this.fromDate = '';
    this.toDate = '';
    this.page = 0;
    this.sortField = null;
    this.sortDirection = 'asc';

    this.fetchFacturas();
  }

  //albaranes grid's detail
  detailView: 'Albaranes' | 'Contabilización' = 'Albaranes';
  albaranesOptio: 'albaranes' | 'aplicaciones' | 'descuentos' = 'albaranes';
  albaranes: any[] = [];
  backipAlbaranes: any[] = [];
  apalicaciones: any[] = [];
  backupAplicaciones: any[] = [];
  descuentos: any[] = [];
  backupDescuentos: any[] = [];
  moreInfoMessageSuccess: string = '';
  moreInfoMessageIsSuccess: boolean = false;
  moreInfoMessageError: string = '';
  moreInfoMessageIsError: boolean = false;

  setAlbaranesOptio(option: 'albaranes' | 'aplicaciones' | 'descuentos', facnum: number): void {
    this.albaranesOptio = option;
    this.limpiarMEssages();
    this.isLoading = true;
    this.albaranes = [];
    this.apalicaciones = [];
    this.descuentos = [];

    if ( option === 'albaranes') {
      this.http.get<any>(`${environment.backendUrl}/api/alb/albaranes/${this.entcod}/${this.eje}/${facnum}`).subscribe({
        next: (response) => {
          if (!Array.isArray(response) || response.length === 0) {
            this.moreInfoMessageIsSuccess = true;
            this.moreInfoMessageSuccess = 'No hay albaranes por las medidas de búsqueda';
            this.albaranes = []
            this.isLoading = false;
            this.pageAlbaranes = 0;
          } else {
            this.albaranes = response;
            this.backipAlbaranes = Array.isArray(response) ? [...response] : [];
            this.isLoading = false;
            this.pageAlbaranes = 0;
          }
        }, error: (err) => {
          this.moreInfoMessageIsError = true;
          this.moreInfoMessageError = err.error.error ?? err.error;
          this.isLoading = false;
          this.pageAlbaranes = 0;
        }
      });
    }

    if ( option === 'aplicaciones') {
      this.http.get<any>(`${environment.backendUrl}/api/fde/${this.entcod}/${this.eje}/${facnum}`).subscribe({
        next: (response) => {
          if (!Array.isArray(response) || response.length === 0) {
            this.moreInfoMessageIsSuccess = true;
            this.moreInfoMessageSuccess = 'No hay aplicaciones por las medidas de búsqueda';
            this.apalicaciones = []
            this.isLoading = false;
            this.pageAplicaiones = 0
          } else {
            this.apalicaciones = response;
            this.backupAplicaciones = Array.isArray(response) ? [...response] : [];
            this.isLoading = false;
            this.pageAplicaiones = 0
          }
        }, error: (err) => {
          this.moreInfoMessageIsError = true;
          this.moreInfoMessageError = err.error.error ?? err.error;
          this.isLoading = false;
          this.pageAplicaiones = 0
        }
      });
    }

    if ( option === 'descuentos') {
      this.http.get<any>(`${environment.backendUrl}/api/fdt/${this.entcod}/${this.eje}/${facnum}`).subscribe({
        next: (response) => {
          if (!Array.isArray(response) || response.length === 0) {
            this.moreInfoMessageIsSuccess = true;
            this.moreInfoMessageSuccess = 'No hay descuentos por las medidas de búsqueda';
            this.descuentos = []
            this.isLoading = false;
            this.pageDescuentos = 0;
          } else {
            this.descuentos = response;
            this.backupDescuentos = Array.isArray(response) ? [...response] : [];
            this.isLoading = false;
            this.pageDescuentos = 0;
          }
        }, error: (err) => {
          this.moreInfoMessageIsError = true;
          this.moreInfoMessageError = err.error.error ?? err.error;
          this.isLoading = false;
          this.pageDescuentos = 0;
        }
      });
    }
  }
  pageAlbaranes = 0;
  get paginatedAlbaranes(): any[] {if (!this.albaranes || this.albaranes.length === 0) return []; const start = this.pageAlbaranes * this.pageSize; return this.albaranes.slice(start, start + this.pageSize);}
  get totalPagesAlbaranes(): number {return Math.max(1, Math.ceil((this.albaranes?.length ?? 0) / this.pageSize));}
  prevPageAlbaranes(): void {if (this.pageAlbaranes > 0) this.pageAlbaranes--;}
  nextPageAlbaranes(): void {if (this.pageAlbaranes < this.totalPagesAlbaranes - 1) this.pageAlbaranes++;}
  goToPageAlbaranes(event: any): void { const inputPage = Number(event.target.value); 
    if (inputPage >= 1 && inputPage <= this.totalPagesAlbaranes) {this.pageAlbaranes = inputPage - 1;}
  }
  pageAplicaiones = 0;
  get paginatedApiciones(): any[] {if (!this.apalicaciones || this.apalicaciones.length === 0) return []; const start = this.pageAlbaranes * this.pageSize; return this.apalicaciones.slice(start, start + this.pageSize);}
  get totalPagesAplicaciones(): number {return Math.max(1, Math.ceil((this.apalicaciones?.length ?? 0) / this.pageSize));}
  prevPageAplicaiones(): void {if (this.pageAplicaiones > 0) this.pageAplicaiones--;}
  nextPageAplicaciones(): void {if (this.pageAplicaiones < this.totalPagesAplicaciones - 1) this.pageAplicaiones++;}
  goToPageAplicaciones(event: any): void { const inputPage = Number(event.target.value); 
    if (inputPage >= 1 && inputPage <= this.totalPagesAplicaciones) {this.pageAplicaiones = inputPage - 1;}
  }
  pageDescuentos = 0;
  get paginatedDescuentos(): any[] {if (!this.descuentos || this.descuentos.length === 0) return []; const start = this.pageAlbaranes * this.pageSize; return this.descuentos.slice(start, start + this.pageSize);}
  get totalPagesDescuentos(): number {return Math.max(1, Math.ceil((this.descuentos?.length ?? 0) / this.pageSize));}
  prevPageDescuentos(): void {if (this.pageDescuentos > 0) this.pageDescuentos--;}
  nextPageDescuentos(): void {if (this.pageDescuentos < this.totalPagesDescuentos - 1) this.pageDescuentos++;}
  goToPageDescuentos(event: any): void { const inputPage = Number(event.target.value); 
    if (inputPage >= 1 && inputPage <= this.totalPagesDescuentos) {this.pageDescuentos = inputPage - 1;}
  }
  
  //misc 
  limpiarMEssages() {
    this.estadoMessage = '';
    this.filterFacturaMessage = '';
    this.moreInfoMessageSuccess = '';
    this.moreInfoMessageError = '';
    this.moreInfoMessageError = '';
    this.moreInfoMessageSuccess = '';
    this.moreInfoMessageSuccess = '';
    this.moreInfoMessageError = '';
  }
}