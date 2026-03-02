import { Component, HostListener } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../sidebar/sidebar.component';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-consulta-proveedores',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './consulta-proveedores.component.html',
  styleUrls: ['./consulta-proveedores.component.css']
})

export class ConsultaProveedoresComponent {
  private entcod: number | null = null;

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

  proveedores: any[] = [];
  private backupProveedores: any[] = [];
  error: string | null = null;
  isLoading: boolean = false;
  ngOnInit(): void {
    this.limpiarMessages();
    this.error = '';
    this.isLoading = true;
    const entidad = sessionStorage.getItem('Entidad');

    if (entidad) {
      const parsed = JSON.parse(entidad);
      this.entcod = parsed.ENTCOD || parsed.entcod;
    }

    if (!entidad || this.entcod === null) {
      sessionStorage.clear();
      alert('Debes iniciar sesión para acceder a esta página.');
      this.router.navigate(['/login']);
      return;
    }

    this.http.get<any>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}`)
      .subscribe({
        next: (response) => {
          if (response.error) {
            this.error = `Error:  ${response.error}`;
          } else {
            this.proveedores = response;
            this.backupProveedores = Array.isArray(response) ? [...response] : [];
            this.defaultProveedores = [...this.backupProveedores];
            this.page = 0;
            this.updatePaginatedProveedores();
            this.isLoading = false;
          }
        },
        error: (err) => {
          this.error = err.error.error ?? err.error;
          this.isLoading = false;
        }
      });
  }

  //sorting main table related function
  sortField: 'tercod' | 'ternom' | 'ternif' | 'terali' | 'tertel' | 'terdom' | 'tercpo' | 'terpob' | 'terayt' | 'terfax' | 'terweb' | 'tercoe' | 'terobs' | null = null;
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
    this.updatePaginatedProveedores();
  }

  private applySort(): void {
    if (!this.sortColumn) return;
    this.proveedores.sort((a, b) => {
      let aValue = a[this.sortColumn];
      let bValue = b[this.sortColumn];

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

  private updatePaginatedProveedores(): void {
    const total = this.totalPages;
    if (total === 0) {
      this.page = 0;
      return;
    }
    if (this.page >= total) {
      this.page = total - 1;
    }
  }

  page = 0;
  pageSize = 20;
  selectedProveedor: any = null;
  get filteredProveedores() {
    let filtered = this.proveedores;
    return filtered;
  }

  get totalPages(): number {
  return Math.ceil(this.proveedores.length / this.pageSize);
  }

  get paginatedProveedores() {
    const start = this.page * this.pageSize;
   return this.proveedores.slice(start, start + this.pageSize);
  }

  nextPage() {
    if (this.page + 1 < this.totalPages) {
      this.page++;
    }
  }

  prevPage() {
    if (this.page > 0) {
      this.page--;
    }
  }

  goToPage(event: any) {
    const inputPage = Number(event.target.value);
    if (inputPage >= 1 && inputPage <= this.totalPages) {
      this.page = inputPage - 1;
    }
  }

  onSearchChange() {
    this.page = 0;
  }
  onFilterChange() {
    this.page = 0;
  }

  showDetails(proveedor: any) {
    this.limpiarMessages();
    this.selectedProveedor = proveedor;
    this.error = '';
    this.showContactPersons(this.selectedProveedor);
  }

  closeDetails() {
    this.limpiarMessages();
    this.activeDetailTab = null;
    this.selectedProveedor = null;
    this.contactPersons = null;
    this.articulos = null;
    this.showContactPersonsGrid = false;
    this.showArticulosGrid = false;
    this.page = 0;
  }

  onSearchFormSubmit(event: Event) {
    event.preventDefault();
    this.search();
  }

  //proveedores main related functions
  searchTerm: string = '';
  filterOption: string = 'noBloqueados';
  search() {
    this.limpiarMessages();
    this.isLoading = true
    if (this.searchTerm.trim() === '') {
      if (this.filterOption === 'Bloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/filter/${this.entcod}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores no bloqueados con el código ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      } else if (this.filterOption === 'noBloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/filter-no/${this.entcod}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores no bloqueados con el código ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      }
    }

    if(/^\d+$/.test(this.searchTerm)) {
      if(this.filterOption === 'noBloqueados') {
        if ((this.searchTerm.length <= 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-tercod-no-bloqueado/${this.entcod}/tercod/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores no bloqueados con el código ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        } else if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-no-bloqueado/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores no bloqueados con el Nif ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        }
      } else if(this.filterOption === 'Bloqueados') {
        if ((this.searchTerm.length <= 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-tercod-bloqueado/${this.entcod}/tercod/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores bloqueados con el codigo ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        } if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-bloquado/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores bloqueados con el NIF ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        }
      } else if(this.filterOption === 'Todos') {
        if ((this.searchTerm.length <= 5)){
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/tercod/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores con el codigo ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        } else if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              if (response.length === 0) {
                this.error = 'No se encontraron proveedores con el NIF ingresado.';
                this.isLoading = false;
              }
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error.error ?? err.error;
              this.isLoading = false;
            }
          })
        }
      }
    } else if (/^[a-zA-Z0-9]+$/.test(this.searchTerm)) {
      if(this.filterOption === 'noBloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-nif-nom-ali-no-bloquado/${this.entcod}/search-by-term?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores no bloqueados con el valor ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      } if(this.filterOption === 'Bloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-nom-ali-bloquado/${this.entcod}/search?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores no bloqueados con el valor ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      } if(this.filterOption === 'Todos') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/search-todos?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores bloqueados con el valor ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      }
    } else {
      if(this.filterOption === 'noBloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-nom-ali-no-bloquado/${this.entcod}/findMatchingNomOrAli?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores no bloqueados con el valor ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      }
      if(this.filterOption === 'Bloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-nom-ali-bloquado/${this.entcod}/searchByNomOrAli?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            if (response.length === 0) {
              this.error = 'No se encontraron proveedores bloqueados con el valor ingresado.';
              this.isLoading = false;
            }
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error.error ?? err.error;
            this.isLoading = false;
          }
        })
      }
    }
  }

  clearSearch() {
    this.limpiarMessages();
    this.proveedores = [...this.backupProveedores];
    this.searchTerm = '';
    this.page = 0;
  }
  
  DownloadPDF() {
    this.limpiarMessages();
    const doc = new jsPDF({orientation: 'landscape', unit: 'mm', format: 'a4'});
    const columns = [
      { header: 'Código', dataKey: 'tercod' },
      { header: 'Nombre', dataKey: 'ternom' },
      { header: 'NIF', dataKey: 'ternif' },
      { header: 'Alias', dataKey: 'terali' },
      { header: 'Teléfono', dataKey: 'tertel' },
      { header: 'Domicilio', dataKey: 'terdom' },
      { header: 'Código Postal', dataKey: 'tercpo' },
      { header: 'Municipio', dataKey: 'terpob' },
      { header: 'Código contable', dataKey: 'terayt' },
      { header: 'Fax', dataKey: 'terfax' },
      { header: 'Web', dataKey: 'terweb' },
      { header: 'Correo electronico', dataKey: 'tercoe' },
      { header: 'Observaciones', dataKey: 'terobs' }
    ];
    const data = this.proveedores;

    autoTable(doc, {
      columns,
      body: data,
      styles: { fontSize: 8 },
      tableWidth: 'wrap',
      headStyles: {
        fillColor: [240, 240, 240],
        textColor: [33, 53, 71],
        fontStyle: 'bold',
        halign: 'left'
      },
      columnStyles: {
        tercod: { cellWidth: 12 },         
        ternom: { cellWidth: 25 },  
        ternif: { cellWidth: 18 },         
        terali: { cellWidth: 25 },          
        tertel: { cellWidth: 18 },          
        terdom: { cellWidth: 25 },        
        tercpo: { cellWidth: 18 },          
        terpob: { cellWidth: 25 },          
        terayt: { cellWidth: 18 },       
        terfax: { cellWidth: 18 },
        terweb: { cellWidth: 20 },
        tercoe: { cellWidth: 25 },
        terobs: { cellWidth: 25 }
      },
      didDrawPage: (dataArg) => {
        doc.setFontSize(10);
        doc.text('Lista de Proveedores', 14, 10);
      }
    });

    doc.save('proveedores.pdf');
  }

  DownloadCSV() {
    this.limpiarMessages();

    const source = this.backupProveedores.length ? this.backupProveedores : this.proveedores;
    if (!source?.length) {
      this.error = 'No hay datos para exportar.';
      return;
    }

    const rows = source.map((row: any, index: number) => ({
      index: index + 1,
      tercod: row.tercod ?? '',
      ternom: row.ternom ?? '',
      ternif: row.ternif ?? '',
      terali: row.terali ?? '',
      tertel: row.tertel ?? '',
      terdom: row.terdom ?? '',
      tercpo: row.tercpo ?? '',
      terpob: row.terpob ?? '',
      terayt: row.terayt ?? '',
      terfax: row.terfax ?? '',
      terweb: row.terweb ?? '',
      tercoe: row.tercoe ?? '',
      terobs: row.terobs ?? ''
    }));

    const columns = [
      { header: '#', key: 'index' },
      { header: 'Código', key: 'tercod' },
      { header: 'Nombre', key: 'ternom' },
      { header: 'NIF', key: 'ternif' },
      { header: 'Alias', key: 'terali' },
      { header: 'Teléfono', key: 'tertel' },
      { header: 'Domicilio', key: 'terdom' },
      { header: 'Código Postal', key: 'tercpo' },
      { header: 'Municipio', key: 'terpob' },
      { header: 'Código contable', key: 'terayt' },
      { header: 'Fax', key: 'terfax' },
      { header: 'Web', key: 'terweb' },
      { header: 'Correo electrónico', key: 'tercoe' },
      { header: 'Observaciones', key: 'terobs' }
    ];

    const csvRows = [
      columns.map(col => `"${col.header}"`).join(';'),
      ...rows.map(row =>
        columns
          .map(col => `"${row[col.key as keyof typeof row] ?? ''}"`)
          .join(';')
      )
    ];

    const csvContent = csvRows.join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'proveedores.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  messageError: string = '';

  //persona related functions
  showPersonasGrid = false;
  personaInfo: any = [];
  showPersonas() {
    this.limpiarMessages();
    this.showPersonasGrid = true;
    this.personaInfo = this.selectedProveedor;
  }

  hidePersonas() {
    this.showPersonasGrid = false;
  }

  showContactPersonsGrid = false;
  contactPersons: any = null;
  activeDetailTab: 'contact' | 'articulo' | null = null;
  personasContactoErrorMessage: string = '';
  showContactPersons(proveedore: any){
    this.limpiarMessages();
    this.showContactPersonsGrid = true;
    this.activeDetailTab = 'contact';
    this.showArticulosGrid = false;
    this.selectedProveedor = proveedore;
    const tercod = proveedore.tercod;

    this.isLoading = true;
    this.http.get<any[]>(`${environment.backendUrl}/api/more/by-tpe/${this.entcod}/${tercod}`)
      .subscribe({ next: (response) => {
        const respArray = Array.isArray(response) ? response : (response ? [response] : []);
        if (respArray.length === 0) { 
          this.contactPersons = null;           
          this.personasContactoErrorMessage = 'No se encontraron personas de contacto.';
          this.isLoading = false;
        } else {
          this.contactPersons = respArray;
          this.personasContactoErrorMessage = '';
          this.isLoading = false;
        }
          this.pagePersona = 0;
      },
      error: (err) => {
        this.personasContactoErrorMessage = err.error.error ?? err.error;
        this.contactPersons = [];
        this.pagePersona = 0;
        this.isLoading = false;
      } 
    });
  }
  pagePersona = 0;
  get filteredPersonas() { let filtered = this.contactPersons; return filtered;}
  get totalPagesPersonas(): number { return Math.ceil(this.contactPersons.length / this.pageSize);}
  get paginatedPersonas() {const start = this.pagePersona * this.pageSize; return this.contactPersons.slice(start, start + this.pageSize);}
  nextPagePersona() {if (this.pagePersona + 1 < this.totalPagesPersonas) {this.pagePersona++;}}
  prevPagePersona() {if (this.pagePersona > 0) {this.pagePersona--;}}
  goToPagePersona(event: any) { const inputPage = Number(event.target.value); if (inputPage >= 1 && inputPage <= this.totalPagesPersonas) {this.pagePersona = inputPage - 1;}}

  //articulos related functions
  showArticulosGrid = false;
  articulos: any = null;
  articuloError: string = '';
  showArticulos(proveedore: any){
    this.limpiarMessages();
    this.showArticulosGrid = true;
    this.activeDetailTab = 'articulo';
    this.showContactPersonsGrid = false;
    this.selectedProveedor = proveedore;
    const tercod = proveedore.tercod;
    
    this.isLoading = true;
    this.http.get<any[]>(`${environment.backendUrl}/api/more/by-apr/${this.entcod}/${tercod}`)
      .subscribe({ next: (response) => {
        this.articulos = Array.isArray(response) ? response : (response ? [response] : []);

        this.articulos.forEach((row: any, index: number) => {
          const artcod = String(row?.artcod ?? '').trim();
          const afacod = String(row?.afacod ?? '').trim();
          const asucod = String(row?.asucod?? '').trim();
          this.getDescription(row, index, artcod, afacod, asucod);
        });
        this.isLoading = false;
        if (response.length === 0) {
          this.articuloError = 'No se encontraron artículos.';
          this.isLoading = false;
        }
        this.pageArticulo = 0;
      },
      error: (err) => {
        this.articuloError = err.error.error ?? err.error;
        this.isLoading = false;
      } 
    });
  }
  pageArticulo = 0;
  get filteredArticulo() { let filtered = this.articulos; return filtered;}
  get totalPagesArticulos(): number { return Math.ceil(this.articulos.length / this.pageSize);}
  get paginatedArticulos() {const start = this.pageArticulo * this.pageSize; return this.articulos.slice(start, start + this.pageSize);}
  nextPageArticulos() {if (this.pageArticulo + 1 < this.totalPagesArticulos) {this.pageArticulo++;}}
  prevPageArticulos() {if (this.pageArticulo > 0) {this.pageArticulo--;}}
  goToPageArticulos(event: any) { const inputPage = Number(event.target.value); if (inputPage >= 1 && inputPage <= this.totalPagesArticulos) {this.pageArticulo = inputPage - 1;}}

  getDescription(row: any, index: number, artcod: string, afacod: string, asucod: string){
    if (artcod === '*') {
      if (asucod === '*') {
        this.http.get<any[]>(`${environment.backendUrl}/api/afa/art-name/${this.entcod}/${afacod}`).subscribe({
          next: (response) => {
            const respArray = Array.isArray(response) ? response : response ? [response] : [];
            const afades = respArray[0]?.afades;
            row.description = String(afades ?? '').trim();
          },
        });
      } else {
        this.http.get<any[]>(`${environment.backendUrl}/api/asu/art-name/${this.entcod}/${afacod}/${asucod}`).subscribe({
          next: (response) => {
            const respArray = Array.isArray(response) ? response : response ? [response] : [];
            const asudes = respArray[0]?.asudes;
            row.description = String(asudes ?? '').trim();
          },
        });
      }
    } else {
      this.http.get<any[]>(`${environment.backendUrl}/api/art/art-name/${this.entcod}/${afacod}/${asucod}/${artcod}`).subscribe({
        next: (response) => {
          const respArray = Array.isArray(response) ? response : response ? [response] : [];
          const artdes = respArray[0]?.artdes;
          row.description = String(artdes ?? '').trim();
        },
      });
    }
  }

  //main table width adjustment related functions
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

  //misc
  limpiarMessages() {
    this.error = '';
    this.messageError = '';
    this.personasContactoErrorMessage = '';
    this.articuloError = '';
  }
}