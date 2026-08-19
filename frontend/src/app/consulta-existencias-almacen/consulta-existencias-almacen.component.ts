import { Component, HostListener} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, JsonPipe } from '@angular/common';
import { SidebarComponent } from '../sidebar/sidebar.component';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { environment } from '../../environments/environment';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-consulta-existencias-almacen',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './consulta-existencias-almacen.component.html',
  styleUrls: ['./consulta-existencias-almacen.component.css']
})
export class ConsultaExistenciasAlmacenComponent {
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
  cge: string = '';
  percod: string = '';
  eje: string = '';
  page = 0;
  pageSize = 20;

  constructor(private http: HttpClient, private router: Router) {}

  existenciasSuccess: string = '';
  existenciasError: string = '';
  ngOnInit(): void{
    this.limpiarMessages();
    const entidad = sessionStorage.getItem('Entidad');
    const centroGestor = sessionStorage.getItem('CENTROGESTOR');
    const percodNam = sessionStorage.getItem('USUCOD');
    const ejercicio = sessionStorage.getItem('EJERCICIO');

    if (entidad) {const parsed = JSON.parse(entidad); this.entcod = parsed.ENTCOD;}
    if (centroGestor) {const parsed = JSON.parse(centroGestor); this.cge = parsed.value;}
    if (percodNam) {this.percod = percodNam;}
    if (ejercicio) {const parsed = JSON.parse(ejercicio); this.eje = parsed.eje;}

    if (!entidad || this.entcod === null || this.cge === '' || this.percod === '' || this.eje === '') {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }

    this.fetchExistencias();
  }

  existencias: any[] = [];
  almacenes: any[] = [];
  isLoading: boolean = false;
  magcod_main: string = '';
  selectedAlmacenNombreSearch: string = '';
  is_main_fetch: boolean = true;
  fetchExistencias() {
    this.isLoading = true;
    this.http.get<any>(`${environment.backendUrl}/api/mea/existencias-almacen/${this.entcod}?cge=${this.cge}&percod=${this.percod}&eje=${this.eje}&page=${this.page}`).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.existencias = res.existencias;
        this.almacenes = res.almacenes;
        this.magcod_main = this.almacenes[0]?.depcod;
        this.selectedAlmacenNombreSearch = this.almacenes[0]?.dep_DEPDES;
        this.fetchTotalPages();
      },
      error: (err) => {
        this.isLoading = false;
        this.existencias = [];
        this.almacenes = [];
        this.existenciasError = err.error.error ?? err.error;
      }
    })
  }
  totalPagesMain = 0;
  fetchTotalPages() {
    this.http.get(`${environment.backendUrl}/api/mea/get-pag/${this.entcod}/${this.magcod_main}`).subscribe({
      next: (res) => {
        this.totalPagesMain = Math.ceil(2650 / 20);
      },
      error: (err) => {
        this.totalPagesMain = 0;
        console.warn(err.error.error ?? err.error);
      }
    })
  }
  almacenChange() {
    this.isLoading = true;
    this.http.get<any>(`${environment.backendUrl}/api/mea/existencias-almacen/${this.entcod}?cge=${this.cge}&percod=${this.percod}&eje=${this.eje}&magcod_main=${this.magcod_main}`).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.existencias = res.existencias;
      },
      error: (err) => {
        this.isLoading = false;
        this.existencias = [];
        this.existenciasError = err.error.error ?? err.error;
      }
    })
  }
  get paginatedExistencias(): any[] {
    return this.existencias ?? [];
  }
  prevPage(): void {
    this.page--;
    this.fetchExistencias();
  }
  nextPage(): void {
    this.page++;
    this.fetchExistencias();
  }

  main_search: string = '';
  afacod: string = '';
  asucod: string = '';
  is_search: boolean = false;
  totalPagesSearch = 0;
  search() {
    if (this.main_search === '' && this.afacod === '' && this.asucod === '') {
      this.almacenChange();
      this.fetchTotalPages();
      return;
    }

    this.is_main_fetch = false;
    this.is_search = true;

    let params = new HttpParams();
    
    if (this.main_search?.trim()) {
      params = params.set('main_search', this.main_search.trim());
    }
    if (this.afacod?.trim()) {
      params = params.set('afaCod', this.afacod.trim());
    }
    if (this.asucod?.trim()) {
      params = params.set('asuCod', this.asucod.trim());
    }
    
    console.log("here")
    params = params.set('page', this.page.toString());
    this.http.get<any>(`${environment.backendUrl}/api/mea/existencias-almacen/${this.entcod}?cge=${this.cge}&percod=${this.percod}&eje=${this.eje}&magcod_main=${this.magcod_main}`, { params }).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.existencias = res.existencias;
        this.totalPagesSearch = Math.ceil(this.existencias.length / this.pageSize);
      },
      error: (err) => {
        this.isLoading = false;
        this.existencias = [];
        this.almacenes = [];
        this.existenciasError = err.error.error ?? err.error;
      }
    })
  }
  get paginatedSearchExistencias(): any[] {
    if (!this.existencias || this.existencias.length === 0) return [];

    const start = this.page * this.pageSize;

    return this.existencias.slice(start, start + this.pageSize);
  }
  prevPageSearch(): void {
    if (this.page > 0) this.page--;
  }
  nextPageSearch(): void {
    if (this.page < this.totalPagesSearch - 1) this.page++;
  }
  goToPageSearch(event: any): void {
    const inputPage = Number(event.target.value);
    if (inputPage >= 1 && inputPage <= this.totalPagesSearch) {
      this.page = inputPage - 1;
    }
  }

  clearSearch() {
    this.limpiarMessages();
    this.main_search = '';
    this.afacod = '';
    this.asucod= '';
    this.page = 0;
    this.is_main_fetch = true;
    this.is_search = false;
    this.fetchExistencias();
  }

  //main functions
  sortField: 'art_AFACOD' | 'art_ASUCOD' | 'art_ARTCOD' | 'art_ARTDES' | 'art_ARTREF' | 'selectedAlmacenNombreSearch' | 'meauni' | 'measol' | 'mearec' | 'calculateKEstVir' | 'mealoc' | null = null;
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  toggleSort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applySort();
    this.page = 0;
  }

  private applySort(): void {
    if (!this.sortColumn) return;
    
    const parseValue = (val: string) => {
      return val.replace(/(\d+)/g, (match) => {
        return match.padStart(20, '0');
      });
    };

    this.existencias.sort((a, b) => {
      const aValue = parseValue((a[this.sortColumn] ?? '').toString().toUpperCase());
      const bValue = parseValue((b[this.sortColumn] ?? '').toString().toUpperCase());
      
      const comparison = aValue.localeCompare(bValue);
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
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
    const table = document.querySelector('.main-table') as HTMLTableElement;
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

  kestvir: number | null = null;
  calculateKEstVir(artuni: number, artsol: number, artrec: number) {
    this.kestvir = artuni - artsol + artrec;
    return this.kestvir;
  }

  //misc
  limpiarMessages() {
    this.existenciasSuccess = '';
    this.existenciasError = '';
  }
}
