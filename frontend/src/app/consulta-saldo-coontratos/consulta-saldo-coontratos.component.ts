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
  selector: 'app-consulta-saldo-coontratos',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './consulta-saldo-coontratos.component.html',
  styleUrls: ['./consulta-saldo-coontratos.component.css']
})

export class ConsultaSaldoCoontratosComponent {
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
  contratos: any = [];
  page = 0;
  pageSize = 20;

  constructor(private http: HttpClient, private router: Router) {}

  isLoading: boolean = false;
  ContratosSuccess: string = '';
  ContratosError: string = '';
  ngOnInit(): void{
    this.limpiarMessages();
    const entidad = sessionStorage.getItem('Entidad');
    const eje = sessionStorage.getItem('EJERCICIO'); 

    if (entidad) {const parsed = JSON.parse(entidad); this.entcod = parsed.ENTCOD;}
    if (eje) {const parsed = JSON.parse(eje); this.eje = parsed.eje;}

    if (!entidad || this.entcod === null || !eje || this.eje === null ) {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }

    this.fetchContratos();
  }

  fetchContratos() {
    this.isLoading = true;
    this.http.get(`${environment.backendUrl}/api/cog/Saldo-contrato/${this.entcod}/${this.eje}`).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.contratos = res;
      },
      error: (err) => {
        this.isLoading = false;
        this.ContratosError = err.error.error || err.error;
      }
    })
  }
  get paginatedContratos(): any[] { if (!this.contratos || this.contratos.length === 0) return [];
    const start = this.page * this.pageSize; return this.contratos.slice(start, start + this.pageSize);
  }
  get totalPages(): number {return Math.max(1, Math.ceil((this.contratos?.length ?? 0) / this.pageSize)); }
  prevPage(): void {if (this.page > 0) this.page--; }
  nextPage(): void {if (this.page < this.totalPages - 1) this.page++;}
  goToPage(event: any): void {const inputPage = Number(event.target.value);
    if (inputPage >= 1 && inputPage <= this.totalPages) {this.page = inputPage - 1;}
  }
  private updatePagination(): void {const total = this.totalPages;
    if (total === 0) {this.page = 0;return;}
    if (this.page >= total) {this.page = total - 1;}
  }

  toggleSort(field: 'concod' | 'cot.conn.conlot' | 'cot.conn.condes' | 'cot.tercod' | 'cot.ter.ternom' | 'cot.ter.ternif' | 'cgecod' | 'cge.cgedes' | 'cogopd' | 'cogop2' | 'calculateSaldoTotal' | 'cogiap' | 'calculateSaldo'): void {
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

  sortField: 'concod' | 'cot.conn.conlot' | 'cot.conn.condes' | 'cot.tercod' | 'cot.ter.ternom' | 'cot.ter.ternif' | 'cgecod' | 'cge.cgedes' | 'cogopd' | 'cogop2' | 'calculateSaldoTotal' | 'cogiap' | 'calculateSaldo' | null = null;
  sortDirection: 'asc' | 'desc' = 'asc';
  private applySort(): void {
    if (!this.sortField) {
      return;
    }

    const field = this.sortField;
    const direction = this.sortDirection === 'asc' ? 1 : -1;

    const getValue = (item: any, path: string): string | number => {
      if (path === 'calculateSaldoTotal') {
        return (item?.cogimp ?? 0) + (item?.cogim2 ?? 0);
      }

      if (path === 'calculateSaldo') {
        return (item?.cogimp ?? 0)
          + (item?.cogim2 ?? 0)
          - (item?.cogiap ?? 0);
      }

      return path
        .split('.')
        .reduce((value, property) => value?.[property], item) ?? '';
    };

    this.contratos = [...this.contratos].sort((first, second) => {
      const firstValue = getValue(first, field);
      const secondValue = getValue(second, field);

      if (typeof firstValue === 'number' && typeof secondValue === 'number') {
        return (firstValue - secondValue) * direction;
      }

      return new Intl.Collator('es', {
        numeric: true,
        sensitivity: 'base'
      }).compare(
        String(firstValue),
        String(secondValue)
      ) * direction;
    });

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

  calculateSaldoTotal(cogimp: number, cogim2: number): number {
    return cogimp + cogim2;
  }

  calculateSaldo(cogimp: number, cogim2: number, cogiap: number): number {
    return (cogimp + cogim2) - cogiap;
  }

  //misc
  limpiarMessages() {
    this.ContratosSuccess = '';
    this.ContratosError = '';
  }
}
