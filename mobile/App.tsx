import {
  Text,
  View,
} from 'react-native';
import {
  SafeAreaProvider,
} from 'react-native-safe-area-context';

function App() {

  return (
    <SafeAreaProvider>
      <AppContent />
    </SafeAreaProvider>
  );
}

function AppContent() {

  return (
    <View>
      <Text>Hello World!</Text>
    </View>
  );
}



export default App;
