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
  selector: 'app-proveedorees',
  standalone: true,
  imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './proveedorees.component.html',
  styleUrls: ['./proveedorees.component.css']
})
export class ProveedoreesComponent {
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
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
              this.isLoading = false;
            }
          })
        } else if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-no-bloqueado/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
              this.isLoading = false;
            }
          })
        }
      } else if(this.filterOption === 'Bloqueados') {
        if ((this.searchTerm.length <= 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-tercod-bloqueado/${this.entcod}/tercod/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
              this.isLoading = false;
            }
          })
        } if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-bloquado/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
              this.isLoading = false;
            }
          })
        }
      } else if(this.filterOption === 'Todos') {
        if ((this.searchTerm.length <= 5)){
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/tercod/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
              this.isLoading = false;
            }
          })
        } else if ((this.searchTerm.length > 5)) {
          this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/ternif/${this.searchTerm}`).subscribe({
            next: (response) => {
              this.proveedores = response;
              this.page = 0;
              this.isLoading = false;
            },
            error: (err) => {
              this.error = err.error || err.error.error;
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
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error || err.error.error;
            this.isLoading = false;
          }
        })
      } if(this.filterOption === 'Bloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ternif-nom-ali-bloquado/${this.entcod}/search?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error || err.error.error;
            this.isLoading = false;
          }
        })
      } if(this.filterOption === 'Todos') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-ent/${this.entcod}/search-todos?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error || err.error.error;
            this.isLoading = false;
          }
        })
      }
    } else {
      if(this.filterOption === 'noBloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-nom-ali-no-bloquado/${this.entcod}/findMatchingNomOrAli?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error || err.error.error;
            this.isLoading = false;
          }
        })
      }
      if(this.filterOption === 'Bloqueados') {
        this.http.get<any[]>(`${environment.backendUrl}/api/ter/by-nom-ali-bloquado/${this.entcod}/searchByNomOrAli?term=${this.searchTerm}`).subscribe({
          next: (response) => {
            this.proveedores = response;
            this.page = 0;
            this.isLoading = false;
          },
          error: (err) => {
            this.error = err.error || err.error.error;
            this.isLoading = false;
          }
        })
      }
    }
  }

  clearSearch() {
    this.limpiarMessages();
    this.proveedores = [...this.backupProveedores];
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

  messageSuccess: string = '';
  messageError: string = '';
  saveChanges() {
    this.isUpdating = true;
    this.limpiarMessages();
    const payload ={
      TERWEB : this.selectedProveedor.terweb,
      TEROBS : this.selectedProveedor.terobs,
      TERBLO : this.selectedProveedor.terblo,
      TERACU : this.selectedProveedor.teracu
    }

    this.http.put(`${environment.backendUrl}/api/ter/updateFields/${this.entcod}/${this.selectedProveedor.tercod}`, payload, { responseType: 'text' }
    ).subscribe({
      next: (res) => {
        this.messageSuccess = 'Proveedor actualizado correctamente';
        this.isUpdating = false;
      },
      error: (err) => {
        this.messageError = err.error.error ?? err.error;
        this.isUpdating = false;
      }
    });
  }

  onCheckboxGeneric(event: Event, field: string) {
    const checked = (event.target as HTMLInputElement).checked;
    if (this.selectedProveedor && field in this.selectedProveedor) {
      (this.selectedProveedor as any)[field] = checked ? 1 : 0;
    }
    if (this.articulos && field in this.articulos) {
      (this.articulos as any)[field] = checked ? 1 : 0;
    }
  }

  onApracuChange(articulo: any, event: Event) {
    const input = event.target as HTMLInputElement;
    articulo.apracu = input.checked ? 1 : 0;
  }

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

  personasContactoErrorMessage: string = '';
  personasContactoSuccessMessage: string = '';
  personaContactoaddError: string = '';
  isAdding: boolean = false;
  addPersonas(tpenom: string, tpetel: string, tpetmo: string, tpecoe: string, tpeobs: string) {
    this.limpiarMessages();
    this.isAdding = true;
    const tercod = this.personaInfo.tercod;

    if(!tpenom) {
      this.personaContactoaddError = 'Nombre requirido'
      return;
    }

    const payload = {
      "tpenom" : tpenom,
      "tpetel" : tpetel,
      "tpetmo" : tpetmo,
      "tpecoe" : tpecoe,
      "tpeobs" : tpeobs
    }

    this.http.post(`${environment.backendUrl}/api/more/add/${this.entcod}/${tercod}`, payload, { responseType: 'text' }).subscribe({
      next: (res) => {
        this.personasContactoSuccessMessage = 'Persona Agregado exitosamente';
        this.hidePersonas();
        this.reloadContactPersons();
        this.isAdding = false;
      },
      error: (err) => {
        this.personaContactoaddError = err.error.error ?? err.error;
        this.isAdding = false;
      }
    })
  }

  reloadContactPersons() {
    this.limpiarMessages();
    if (!this.selectedProveedor) return;
    this.showContactPersons(this.selectedProveedor);
  }

  isUpdating: boolean = false;
  updatepersonas(tpecod: number, nom: string, telefon: string, movil: string, email: string, obs: string){
    this.isUpdating = true;
    this.limpiarMessages();
    if (!nom) {
      this.personasContactoErrorMessage = 'El nombre de la persona no debe estar vacío';
      return;
    }
    const updateFields = {
      tpenom : nom,
      tpetel : telefon,
      tpetmo : movil,
      tpecoe : email,
      tpeobs : obs
    }
    this.http.put(
      `${environment.backendUrl}/api/more/modify/${this.entcod}/${this.selectedProveedor.tercod}/${tpecod}`,
      updateFields,
      { responseType: 'text' }
    ).subscribe({
      next: (res) => {
        this.personasContactoErrorMessage = '';
        this.personasContactoSuccessMessage = 'Persona de contacto actualizada correctamente';
        this.isUpdating = false;
      },
      error: (err) => {
        this.personasContactoSuccessMessage = '';
        this.personasContactoErrorMessage = err.error.error ?? err.error;
        this.isUpdating = false;
      }
    });
  }

  deletepersona(persona: any){
    this.limpiarMessages();
    this.isDeleting = true;
    const tercod = persona.tercod;
    const tpecod = persona.tpecod;
    this.http.delete(
      `${environment.backendUrl}/api/more/delete/${this.entcod}/${tercod}/${tpecod}`,
      { responseType: 'text' }).subscribe({
      next: (res) => {
        this.personasContactoSuccessMessage = 'Persona de contacto eliminada correctamente';
        if (Array.isArray(this.contactPersons)) {
          this.contactPersons = this.contactPersons.filter((p: any) =>
            !(p.tercod === tercod && p.tpecod === tpecod)
          );
        }
        this.isDeleting = false;
        this.closeDeletePersonas();
      },
      error: (err) => {
        this.personasContactoErrorMessage = err.error.error ?? err.error;
        this.isDeleting = false;
      }
    });
  }

  showDeletePersona = false;
  personaToDelete: any = null;
  openDeletePersonas(persona: any) {
    this.limpiarMessages();
    this.personaToDelete = persona;
    this.showDeletePersona = true;
  }

  closeDeletePersonas() {
    this.showDeletePersona = false;
    this.personaToDelete = null;
    this.limpiarMessages();
  }

  confirmDeletePersona() {
    if (this.showDeletePersona) {
      this.deletepersona(this.personaToDelete);
    }
  }

  //articulos related functions
  articuloSuccessMessage: string = '';
  showHelloGrid = false;
  selectedFamilia: string = '';
  selectedSubfamilia: string = '';
  selectedArticulo: string = '';
  afas: any[] = [];
  asus: any[] = [];
  arts: any[] = [];
  showHello() {
    this.limpiarMessages();
    this.searchValue = '';
    this.searchResults = [];
    this.searchType = 'familia';
    this.showHelloGrid = true;
    this.selectedFamilia = '';
    this.selectedSubfamilia = '';
    this.selectedArticulo = '';
    this.asus = [];
    this.arts = [];
  }

  hideHello() {
    this.limpiarMessages();
    this.showHelloGrid = false;
    this.searchValue = '';
    this.searchResults = [];
    this.searchType = 'familia';
    this.searchPage;
    this.selectedFamilia = '';
    this.selectedSubfamilia = '';
    this.selectedArticulo = '';
    this.afas = [];
    this.asus = [];
    this.arts = [];
  }

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
          this.anadirmessage = '';
          this.anadirErrorMessage = '';
        });
        this.isLoading = false;
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

  updatearticulos(articulo: any){
    this.limpiarMessages();
    this.isUpdating = true; 

    const tercod = this.selectedProveedor.tercod;
    const afacod = articulo.afacod;
    const asucod = articulo.asucod;
    const artcud = articulo.artcod;

    const updateFields = {
      aprref : articulo.aprref,
      apruem : articulo.apruem,
      aprobs : articulo.aprobs,
      apracu : articulo.apracu,
      aprpre : articulo.aprpre
    }

    this.http.patch(`${environment.backendUrl}/api/more/update-apr/${this.entcod}/${tercod}/${afacod}/${asucod}/${artcud}`,updateFields,{ responseType: 'text' }).subscribe({
      next: (res) => {
        this.articuloError = '';
        this.articuloSuccessMessage = 'Artículo actualizado correctamente';
        this.isUpdating = false;
      },
      error: (err) => {
        this.articuloSuccessMessage = '';
        this.articuloError = err.error.error ?? err.error;
        this.isUpdating = false;
      }
    });
  }

  isDeleting: boolean = false;
  deletearticulo(articulo: any){
    this.isDeleting = true;
    this.limpiarMessages();
    const params = [
    `ent=${articulo.ent}`,
    `tercod=${articulo.tercod}`,
    `afacod=${articulo.afacod}`,
    `asucod=${articulo.asucod}`,
    `artcod=${articulo.artcod}`
  ].join('&');

    this.http.delete(
      `${environment.backendUrl}/api/more/delete-apr?${ params}`,
      { responseType: 'text' }).subscribe({
        next: (res) => {
            this.articuloSuccessMessage = 'Artículo eliminado correctamente';
            this.articulos = this.articulos.filter((a: any) =>
            !(a.ent === articulo.ent &&
              a.tercod === articulo.tercod &&
              a.afacod === articulo.afacod &&
              a.asucod === articulo.asucod &&
              a.artcod === articulo.artcod)
          );
          this.isDeleting = false;
          this.closeDeleteConfirm();
        },
        error: (err) => {
          this.articuloError = err.error.error ?? err.error;
          this.isDeleting = false;
        }
      });
  }

  showDeleteConfirm = false;
  articuloToDelete: any = null;
  openDeleteConfirm(articulo: any) {
    this.articuloToDelete = articulo;
    this.showDeleteConfirm = true;
  }

  closeDeleteConfirm() {
    this.showDeleteConfirm = false;
    this.articuloToDelete = null;
    this.limpiarMessages();
  }

  confirmDelete() {
    if (this.articuloToDelete) {
      this.deletearticulo(this.articuloToDelete);
    }
  }

  anadirErrorMessage: string = '';
  anadirmessage: string = '';
  addArticulo(){
    this.isAdding = true;
    this.limpiarMessages();
    const newArticulo = {
      ent: this.entcod,
      tercod: this.selectedProveedor.tercod,
      afacod: this.selectedFamilia || '*',
      asucod: this.selectedSubfamilia || '*',
      artcod: this.selectedArticulo || '*',
    };

    if ((this.articulos ?? []).some((a: any) =>
      (a.afacod ?? '*') === newArticulo.afacod &&
      (a.asucod ?? '*') === newArticulo.asucod &&
      (a.artcod ?? '*') === newArticulo.artcod)) {
      this.anadirErrorMessage = 'El artículo ya está en la lista.';
      this.isAdding = false;
      return;
    }

    this.http.post(
      `${environment.backendUrl}/api/more/add-apr`,newArticulo, { responseType: 'text' }).subscribe({
      next: (res) => {
        this.anadirmessage = 'Artículo añadido correctamente';

        if (this.showArticulosGrid) {
          const newRow = {
            afacod: this.selectedFamilia || '*',
            asucod: this.selectedSubfamilia || '*',
            artcod: this.selectedArticulo || '*',
            ent: this.entcod,
            tercod: this.selectedProveedor.tercod,
            description: ''
          };
          if (!this.articulos) this.articulos = [];
          this.articulos.push(newRow);

          const artcod = String(newRow.artcod ?? '').trim();
          const afacod = String(newRow.afacod ?? '').trim();
          const asucod = String(newRow.asucod ?? '').trim();
          const index = this.articulos.length - 1;

          this.getDescription(newRow, index, artcod, afacod, asucod);
          this.articuloSuccessMessage = '';
          this.articuloError = '';
        }
        this.isAdding = false;
      },
      error: (err) => {
        this.anadirErrorMessage = err.error.error ?? err.error;
        this.isAdding = false;
      }
    });
  }

  selectedSearchRow: any = null;
  selectSearchRow(item: any) {
    this.selectedSearchRow = item;
    this.selectedFamilia = item.afacod;
    this.selectedSubfamilia = item.asucod;
    this.selectedArticulo = item.artcod;
  }

  searchType: string = 'familia';
  searchValue: string = '';
  searchResults: any[] = [];
  onSearch() {
    this.limpiarMessages();
    this.searchPage = 0;
    this.searchResults = [];
    if (!this.searchValue || !this.searchType) return;
    let url = '';
    if (this.searchType === 'familia') {
      if(/^\d+$/.test(this.searchValue)) {
        url = `${environment.backendUrl}/api/afa/by-ent/${this.entcod}/${this.searchValue}`;
      } else {
        url = `${environment.backendUrl}/api/afa/by-ent-like/${this.entcod}/${this.searchValue}`;
      }
    } else if (this.searchType === 'subfamilia') {
      if(/^\d+$/.test(this.searchValue)) {
        url = `${environment.backendUrl}/api/asu/by-ent/${this.entcod}/${this.searchValue}/${this.searchValue}`;
      } else {
        url = `${environment.backendUrl}/api/asu/by-ent-like/${this.entcod}/${this.searchValue}`;
      }
    } else if (this.searchType === 'articulo') {
      if(/^\d+$/.test(this.searchValue)) {
        url = `${environment.backendUrl}/api/art/by-ent/${this.entcod}/${this.searchValue}/${this.searchValue}/${this.searchValue}`;
      } else {
        url = `${environment.backendUrl}/api/art/by-ent-like/${this.entcod}/${this.searchValue}`;
      }
      
    }
    this.http.get<any[]>(url, { withCredentials: true }).subscribe({
    next: (data) => {
      this.searchResults = data;
    },error: (err) => {
        console.error('Error fetching search results:', err);
        this.searchResults = [];
      }
    });
  }

  searchPage: number = 0;
  searchPageSize: number = 5;
  get paginatedSearchResults() {
    const start = this.searchPage * this.searchPageSize;
    return this.searchResults.slice(start, start + this.searchPageSize);
  }

  get searchTotalPages() {
    return Math.ceil(this.searchResults.length / this.searchPageSize);
  }

  getDescription(row: any, index: number, artcod: string, afacod: string, asucod: string){
    if (artcod === '*') {
      if (asucod === '*') {
        this.http
          .get<any[]>(`${environment.backendUrl}/api/afa/by-ent/${this.entcod}/${afacod}`)
          .subscribe({
            next: (response) => {
              const respArray = Array.isArray(response) ? response : response ? [response] : [];
              const afades = respArray[0]?.afades;
              row.description = String(afades ?? '').trim();
            },
          });
      } else {
        this.http
          .get<any[]>(`${environment.backendUrl}/api/asu/art-name/${this.entcod}/${afacod}/${asucod}`)
          .subscribe({
            next: (response) => {
              const respArray = Array.isArray(response) ? response : response ? [response] : [];
              const asudes = respArray[0]?.asudes;
              row.description = String(asudes ?? '').trim();
            },
          });
      }
    } else {
      this.http
        .get<any[]>(`${environment.backendUrl}/api/art/art-name/${this.entcod}/${afacod}/${asucod}/${artcod}`)
        .subscribe({
          next: (response) => {
            const respArray = Array.isArray(response) ? response : response ? [response] : [];
            const artdes = respArray[0]?.artdes;
            row.description = String(artdes ?? '').trim();
          },
        });
    }
  }

  //adding from webservice proveedores related functions
  showProveedorModal = false;
  anadirProveedorErrorMessage: string = '';
  openProveedorModal(): void {
    this.showProveedorModal = true;
    this.limpiarMessages();
    this.resetProveedorModalState();
    this.onProveedorFetch();
  }

  closeProveedorModal(): void {
    this.showProveedorModal = false;
    this.limpiarMessages();
    this.resetProveedorModalState();
  }

  private resetProveedorModalState(): void {
    this.limpiarMessages();
    this.proveedoresSearchPage = 0;

    if (this.fullProveedoresSearchResults && this.fullProveedoresSearchResults.length) {
      this.proveedoresSearchResults = [...this.fullProveedoresSearchResults];
    } else {
      this.proveedoresSearchResults = [];
    }
  }

  searchAdd: string = 'nif';
  searchProveedor: string = '';
  onProveedorSearchChange(): void {
    const q = (this.searchProveedor || '').toString().trim();
    if (q.length === 0) {
      this.proveedoresSearchResults = [...(this.fullProveedoresSearchResults || [])];
      this.proveedoresSearchPage = 0;
      this.anadirProveedorErrorMessage = '';
    }
  }

  guardarProveedorIssuccess = false;
  proveedoresSearchResults: any[] = [];
  fullProveedoresSearchResults: any[] = [];
  selectedProveediresFromResults: any[] = [];
  onProveedorFetch(){
    const ent = this.entcod;

    this.http.get<any[]>(`${environment.backendUrl}/api/sical/terceros`, { withCredentials: true}).subscribe({
      next:(data) => {
        this.fullProveedoresSearchResults = Array.isArray(data) ? data: [];
        this.proveedoresSearchResults = [...this.fullProveedoresSearchResults];
        const normalized = (Array.isArray(data) ? data : []).map(d => ({
          ENT: ent,
          TERNOM: d.nomTercero ?? '',
          TERALI: d.apellTercero ?? '',
          NIF: d.niftercero ?? d.ternif ?? '',
          TERDOM: d.domicilio ?? '',
          TERCPO: d.codigoPostal ?? '',
          TERTEL: d.telefono ?? '',
          TERFAX: d.fax ?? '',
          TERWEB: d.web ?? '',
          TERCOE: d.email ?? '',
          TEROBS: d.observaciones ?? '',
          TERPOB: d.poblacion ?? '',
          __raw: d
          }));
        this.fullProveedoresSearchResults = normalized;
        this.proveedoresSearchResults = [...normalized];
        if (this.proveedoresSearchResults.length === 0) {
          this.anadirProveedorErrorMessage = 'No se encontraron proveedores.';
        }
      }, error: (err) => {
        this.anadirProveedorErrorMessage = err.error.error ?? err.error;
      }
    });
  }

  onProveedorSearch(){
    this.limpiarMessages();
    const q = (this.searchProveedor || '').toString().trim();
    if (!this.searchAdd) {
      this.anadirProveedorErrorMessage = 'Selecciona tipo de búsqueda';
      return;
    }

    if (q.length === 0) {
      this.proveedoresSearchResults = [...(this.fullProveedoresSearchResults || [])];
      this.proveedoresSearchPage = 0;
      this.anadirProveedorErrorMessage = '';
      return;
    }

    const applyFilter = () => {
      const term = q.toLowerCase();
      let filtered: any[] = [];
      if (this.searchAdd === 'nif') {
        filtered = this.fullProveedoresSearchResults.filter(p => (p.NIF || p.ternif || '').toString().toLowerCase().includes(term));
      } else if (this.searchAdd === 'nom') {
        filtered = this.fullProveedoresSearchResults.filter(p => (p.TERNOM || p.ternom || '').toString().toLowerCase().includes(term));
      } else if (this.searchAdd === 'apell') {
        filtered = this.fullProveedoresSearchResults.filter(p => (p.TERALI || '').toString().toLowerCase().includes(term));
      } else {
        filtered = this.fullProveedoresSearchResults.filter(p =>
          (p.NIF || p.ternif || '').toString().toLowerCase().includes(term) ||
          (p.TERNOM || p.ternom || '').toString().toLowerCase().includes(term) ||
          (p.TERALI || '').toString().toLowerCase().includes(term)
        );
      }

      this.proveedoresSearchResults = filtered;
      this.proveedoresSearchPage = 0;
      this.anadirProveedorErrorMessage = filtered.length === 0 ? 'No se encontraron proveedores.' : '';
    };
    applyFilter();
  }

  private proveedorKey(p: any): string {
    const ent = (p?.ENT ?? '').toString();
    const ternom = (p?.TERNOM ?? p?.nomTercero ?? '').toString().trim();
    const ternif = (p?.NIF ?? p?.TERNIF ?? p?.niftercero ?? '').toString().trim();
    return `${ent}|${ternom}|${ternif}`;
  }

  isProveedorSelected(p: any): boolean {
    if (!this.selectedProveediresFromResults?.length) return false;
    const k = this.proveedorKey(p);
    return this.selectedProveediresFromResults.some(s => this.proveedorKey(s) === k);
   }

  selectProveedor(item: any){
    const k = this.proveedorKey(item);
    const idx = this.selectedProveediresFromResults.findIndex(s => this.proveedorKey(s) === k);
    if (idx === -1) {
      this.selectedProveediresFromResults.push(item);
    } else {
      this.selectedProveediresFromResults.splice(idx, 1);
    }
  }

  clearSelectedProveedores() {
    this.selectedProveediresFromResults = [];
    this.limpiarMessages();
  }

  private proveedorExistsInMainList(candidate: any): boolean {
    const candidateNif = (candidate?.NIF ?? candidate?.TERNIF ?? '').toString().trim().toLowerCase();
    if (!candidateNif) return false;
    return (this.proveedores ?? []).some(main =>
      (main?.ternif ?? '').toString().trim().toLowerCase() === candidateNif
    );
  }

  guardarMessageProveedor: string = '';
  saveProveedorees() {
    this.limpiarMessages();
    const ent = this.entcod;

    if (!this.selectedProveediresFromResults || this.selectedProveediresFromResults.length === 0) {
      this.anadirProveedorErrorMessage = 'No hay proveedores seleccionados';
      return;
    }

    const uniqueSelections = this.selectedProveediresFromResults.filter(p => !this.proveedorExistsInMainList(p));

    if (uniqueSelections.length === 0) {
      this.anadirProveedorErrorMessage = 'Todos los proveedores seleccionados ya existen en la lista.';
      return;
    }

    const payload = this.selectedProveediresFromResults.map(p => ({
      ENT: ent,
      TERNOM: p.TERNOM ?? '',
      TERALI: p.TERALI ?? '',
      TERNIF: p.NIF ?? '',
      TERDOM: p.TERDOM ?? '',
      TERCPO: p.TERCPO ?? '',
      TERTEL: p.TERTEL ?? '',
      TERFAX: p.TERFAX ?? '',
      TERWEB: p.TERWEB ?? '',
      TERCOE: p.TERCOE ?? '',
      TEROBS: p.TEROBS ?? '',
      TERPOB: p.TERPOB ?? '',
    }));

    const token = sessionStorage.getItem('JWT');
    const headers = token
      ? new HttpHeaders({ 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' })
      : new HttpHeaders({ 'Content-Type': 'application/json' });

    this.isSaving = true;
    this.limpiarMessages();
    this.http.post<any[]>(`${environment.backendUrl}/api/ter/save-proveedores/${ent}`, payload, { headers, observe: 'response', responseType: 'text' as 'json' })
      .subscribe({
        next: (res) => {
          this.isSaving = false;
          const savedCount = Array.isArray(res.body) ? res.body.length : this.selectedProveediresFromResults.length;
          this.clearSelectedProveedores();
          this.guardarProveedorIssuccess = true;
          this.guardarMessageProveedor = `Proveedores guardados correctamente (${savedCount}).`;
        },
        error: (err) => {
          this.isSaving = false;
          const msg = err.error.error ?? err.error;  
        }
      })
  }

  isSaving = false;
  proveedoresSearchPage: number = 0;
  proveedoresSearchPageSize: number = 10;
  get paginatedProveedoresSearchResults() {
    const start = this.proveedoresSearchPage * this.proveedoresSearchPageSize;
    return this.proveedoresSearchResults.slice(start, start + this.proveedoresSearchPageSize);
  }

  get proveedoresSearchTotalPages() {
    return Math.ceil(this.proveedoresSearchResults.length / this.proveedoresSearchPageSize) || 1;
  }

  proveedoresSearchPrev() {
    if (this.proveedoresSearchPage > 0) this.proveedoresSearchPage--;
  }

  proveedoresSearchNext() {
    if (this.proveedoresSearchPage + 1 < this.proveedoresSearchTotalPages) this.proveedoresSearchPage++;
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

  //mist
  limpiarMessages() {
    this.error = '';
    this.messageError = '';
    this.messageSuccess = '';
    this.personasContactoErrorMessage = '';
    this.personasContactoSuccessMessage = '';
    this.articuloError = '';
    this.articuloSuccessMessage = '';
    this.personaContactoaddError = '';
    this.anadirmessage = '';
    this.anadirErrorMessage = '';
    this.guardarMessageProveedor = '';
    this.anadirProveedorErrorMessage = '';
  }
}