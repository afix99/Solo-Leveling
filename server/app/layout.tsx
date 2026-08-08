export const metadata = {
  title: "AM SYSTEM",
  description: "Backup and analytics service for the ASCEND app.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0, background: "#08090c", color: "#e8eaf0" }}>{children}</body>
    </html>
  );
}
