import { Component, HostListener } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { environment } from '../../environments/environment';

import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-tipo-almacenaje',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent ],
  templateUrl: './tipo-almacenaje.component.html',
  styleUrls: ['./tipo-almacenaje.component.css']
})
export class TipoAlmacenajeComponent {
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

  constructor(private http: HttpClient, private router: Router) {}

  //global variables
  private entcod: number | null = null;
  almacenajes: any = [];
  page = 0;
  pageSize = 20;
  almacenajesSuccess: string = '';
  almacenajesError: string = '';
  isLoadingAlmacenajes: boolean = false;

  ngOnInit() {
    this.limpiarMessages();
    const entidad = sessionStorage.getItem('Entidad');
    if (entidad) {const parsed = JSON.parse(entidad); this.entcod = parsed.ENTCOD;}
    if (!entidad || this.entcod === null) {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }
    
    this.fetchAlmacenajes();
  }

  //main table functions
  fetchAlmacenajes() {
    this.isLoadingAlmacenajes = true;
    this.http.get(`${environment.backendUrl}/api/mta/all-mta/${this.entcod}`).subscribe({
      next: (res) => {
        this.isLoadingAlmacenajes = false;
        this.almacenajes = res;
        this.page = 0;
      },
      error: (err) => {
        this.isLoadingAlmacenajes = false;
        this.almacenajesError = err.error.error ?? err.error
      }
    })
  }
  get paginatedAlmacenajes(): any[] { if (!this.almacenajes || this.almacenajes.length === 0) return [];
    const start = this.page * this.pageSize; return this.almacenajes.slice(start, start + this.pageSize);
  }
  get totalPages(): number {return Math.max(1, Math.ceil((this.almacenajes?.length ?? 0) / this.pageSize)); }
  prevPage(): void {if (this.page > 0) this.page--; }
  nextPage(): void {if (this.page < this.totalPages - 1) this.page++;}
  goToPage(event: any): void {const inputPage = Number(event.target.value);
    if (inputPage >= 1 && inputPage <= this.totalPages) {this.page = inputPage - 1;}
  }
  private updatePagination(): void {const total = this.totalPages;
    if (total === 0) {this.page = 0;return;}
    if (this.page >= total) {this.page = total - 1;}
  }

  toggleSort(field: 'mtacod' | 'mtades'): void {
    if (this.sortField !== field) {
      this.sortField = field;
      this.sortDirection = 'asc';
    } else if (this.sortDirection === 'asc') {
      this.sortDirection = 'desc';
    } else {
      this.sortField = null;
      this.sortDirection = 'asc';
      this.page = 0;
      this.updatePagination();
      return;
    }

    this.applySort();
  }

  sortField: 'mtacod' | 'mtades' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';
  private applySort(): void {
    if (!this.sortField) {
      return;
    }

    const field = this.sortField;
    const collator = new Intl.Collator('es', { numeric: true, sensitivity: 'base' });

    const sorted = [...this.almacenajes].sort((a, b) => {
      const extract = (item: any, prop: string) =>
        (item?.[prop] ?? item?.[prop.toUpperCase()] ?? '').toString();

      const aVal = extract(a, field);
      const bVal = extract(b, field);
      return this.sortDirection === 'asc'
        ? collator.compare(aVal, bVal)
        : collator.compare(bVal, aVal);
    });

    this.almacenajes = sorted;
    this.page = 0;
    this.updatePagination();
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
    const table = document.querySelector('.ejercicio-table') as HTMLTableElement;
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

  searchTerm: string = '';
  search() {
    const numsOnly = /^\d+$/;
    const term = String(this.searchTerm).trim();

    if (numsOnly.test(term)) {
      this.isLoadingAlmacenajes = true;
      this.http.get(`${environment.backendUrl}/api/mta/mta-filter/${this.entcod}/${term}`).subscribe({
        next: (res) => {
          this.isLoadingAlmacenajes = false;
          this.almacenajes = res;
          this.page = 0;
        },
        error: (err) => {
          this.isLoadingAlmacenajes = false;
          this.almacenajesError = err.error.error ?? err.error
        }
      })
    } else {
      this.isLoadingAlmacenajes = true;
      this.http.get(`${environment.backendUrl}/api/mta/search-almacenaje/${this.entcod}/${term}`).subscribe({
        next: (res) => {
          this.isLoadingAlmacenajes = false;
          this.almacenajes = res;
          this.page = 0;
        },
        error: (err) => {
          this.isLoadingAlmacenajes = false;
          this.almacenajesError = err.error.error ?? err.error
        }
      })
    }

  }

  limpiarSearch() {
    this.limpiarMessages();
    this.searchTerm = '';
    this.fetchAlmacenajes();
  }

  //misc
  limpiarMessages() {
    this.almacenajesSuccess = '';
    this.almacenajesError = '';
  }
}
