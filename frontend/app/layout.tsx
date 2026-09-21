import "./globals.css";
import type { Metadata } from "next";
export const metadata: Metadata = {
  title: "CrossEd · Aprender se cruza con jugar",
  description: "Crucigramas educativos multijugador",
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
