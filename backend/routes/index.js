const goalsRoutes = require('./goals.routes');

const router = express.Router();

router.use('/goals', goalsRoutes);

module.exports = router;