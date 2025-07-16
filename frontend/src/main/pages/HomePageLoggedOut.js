import BasicLayout from "main/layouts/BasicLayout/BasicLayout";

export default function HomePageLoggedOut() {
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Welcome to Frontiers!</h1>
        <p>This is the MVP for the Frontiers Project.</p>
      </div>
    </BasicLayout>
  );
}
