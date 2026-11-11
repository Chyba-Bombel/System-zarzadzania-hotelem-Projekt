public final class RunAllProbes {
    public static void main(String[] args) throws Exception {
        TestDatabase.wymagajTestowejBazy();
        CalendarProbe.main(new String[0]);
        SecurityProbe.main(new String[0]);
        IntegrationProbe.main(new String[0]);
        RoomChangeProbe.main(new String[0]);
        ReportProbe.main(new String[0]);
        PrivilegeProbe.main(new String[0]);
        System.out.println("ALL_PROBES_OK");
    }
}
