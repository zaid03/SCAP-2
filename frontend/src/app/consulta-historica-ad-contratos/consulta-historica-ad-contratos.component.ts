import { Component, HostListener} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, JsonPipe } from '@angular/common';
// import { SidebarComponent } from '../sidebar/sidebar.component';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { environment } from '../../environments/environment';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-consulta-historica-ad-contratos',
  standalone: true,
//   imports: [ CommonModule ,FormsModule, SidebarComponent],
  templateUrl: './consulta-historica-ad-contratos.component.html',
  styleUrls: ['./consulta-historica-ad-contratos.component.css']
})

export class ConsultaHistoricaAdContratosComponent {
//   //3 dots menu 
//   showMenu = false;
//   toggleMenu(event: MouseEvent): void {
//     event.stopPropagation();
//     this.showMenu = !this.showMenu;
//   }

//   @HostListener('document:click')
//   closeMenu(): void {
//     this.showMenu = false;
//   }

//   //global variables
//   private entcod: number | null = null;
//   private eje: number | null = null;
//   private cge: string = '';
//   private percod: string = '';
//   almacenes: any[] = [];
//   page = 0;
//   pageSize = 20;

//   constructor(private http: HttpClient, private router: Router) {}

//   isLoading: boolean = false;
//   historicaContratosSuccess: string = '';
//   historicaContratosError: string = '';
//   ngOnInit(): void{
//     this.limpiarMessages();
//     const entidad = sessionStorage.getItem('Entidad');
//     const eje = sessionStorage.getItem('EJERCICIO'); 
//     const centrogestor = sessionStorage.getItem('CENTROGESTOR');
//     const nombre = sessionStorage.getItem('USUCOD');

//     if (entidad) {const parsed = JSON.parse(entidad); this.entcod = parsed.ENTCOD;}
//     if (eje) {const parsed = JSON.parse(eje); this.eje = parsed.eje;}
//     if (centrogestor) {const parsed = JSON.parse(centrogestor); this.cge = parsed.value;}
//     if (nombre) {this.percod = nombre;}

//     if (!entidad || this.entcod === null || !eje || this.eje === null ) {
//       sessionStorage.clear();
//       alert('Debes iniciar sesión para acceder a esta página.');
//       this.router.navigate(['/login']);
//       return;
//     }

//     this.fetchHistoricaContratos();
//   }

//   fetchHistoricaContratos() {

//   }

//   //misc
//   limpiarMessages() {
//     this.historicaContratosSuccess = '';
//     this.historicaContratosError = '';
//   }
}
